package com.ttProject.xuggle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvAtom;
import com.ttProject.streaming.MediaManager;
import com.ttProject.streaming.tak.TakManager;
import com.ttProject.xuggle.flv.FlvManager;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IVideoPicture;

/**
 * コンバート動作を管理するマネージャー
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここでは、すべてを管理します。
 * 
 * propertiesのファイルで変換は管理します？
 * convertManagerって１つじゃね？
 */
public class ConvertManager {
	/** 各部屋ごとのインスタンス保持 */
	private static final ConvertManager instance = new ConvertManager();
	/** ロガー */
	private final Logger logger = LoggerFactory.getLogger(ConvertManager.class);
	/** 名前保持 */
	private String name;
	public String getName() {
		return name;
	}
	/** 生データのtakStreaming用のmanager */
	private TakManager rawTakManager = null;
	/** コンバートマネージャー保持 */
	private Set<MediaManager> mediaManagers = new HashSet<MediaManager>();
	private Set<VideoEncodeManager> videoEncodeManagers = new HashSet<VideoEncodeManager>();
	private Set<AudioEncodeManager> audioEncodeManagers = new HashSet<AudioEncodeManager>();
	private Set<VideoResampleManager> videoResampleManagers = new HashSet<VideoResampleManager>();
	private Set<AudioResampleManager> audioResampleManagers = new HashSet<AudioResampleManager>();
	/** コンバート用のflvManager */
	private FlvManager flvManager = null;
	public FlvManager getFlvManager() {
		return flvManager;
	}
	/** 変換処理の実行内容 */
	private Transcoder transcoder = null;
	/** 変換処理を実行Thread */
	private Thread transcodeThread = null;
	/** マルチスレッド化用のexecutors */
	private final ExecutorService executors;
	/**
	 * コンストラクタ
	 */
	private ConvertManager() {
		int numThreads = Runtime.getRuntime().availableProcessors() * 2;
		executors = Executors.newFixedThreadPool(numThreads);
	}
	/**
	 * シングルトンのインスタンス取得
	 * @return
	 */
	public static ConvertManager getInstance() {
		if(instance == null) {
			throw new RuntimeException("インスタンスが消滅しました。");
		}
		return instance;
	}
	/**
	 * 初期化
	 */
	public void initialize(String name) {
		logger.info("初期化を実行します。:" + name);
		// 名前を保持しておく。
		this.name = name;
		// encode.xmlから必要な情報を抜き出しておく。
		EncodeXmlAnalizer analizer = EncodeXmlAnalizer.getInstance();
		// このマネージャーリストにそって、コンバートを実行する必要がある。
		List<MediaManager> mediaManagers = analizer.getManagers(); // 作成された、MediaManagerリストを取得する
		
		this.mediaManagers.clear();
		logger.info("managerの中身を確認していきます。");
		// managerの内容を確認する。
		for(MediaManager manager : mediaManagers) {
			// 内容の構築をすすめる。
			manager.analize(); // xmlに内容からデータを構築する。
			manager.setup(); // handlerのオープンを実行(コンテナは別で実行します。)
			if(manager instanceof TakManager) {
				TakManager takManager = (TakManager)manager;
				if(takManager.isRawStream()) {
					// 生データのストリーミングが存在する。
					rawTakManager = takManager;
					// TODO このタイミングでheaderを追加しておいてOK
					continue;
				}
			}
			// mediaManagerのセットに登録しておく。
			this.mediaManagers.add(manager);
		}
		if(this.mediaManagers.size() == 0) {
			// コンバートする必要がないので、ここでおわる。
			logger.info("コンバートする必要がないので、ここで処理を終了します。");
			return;
		}
		logger.info("コンバートを実行するためflvManagerを構築します。" + mediaManagers.size() + "個の変換が存在します。");
		// コンバートを実行する必要がある。
		// FlvManagerをつくる。
		flvManager = new FlvManager();
		// この時点で入力変換用のthreadをつくる必要がある。
		// 要するにTranscoderの部分
		// 実際の内部の初期化は、inputContainerを開く部分は入力変換用のthereadの冒頭
		// 出力コンテナの部分は、入力メディアデータのうち対象のデータをうけとった瞬間に実行(もしくは再構築)する。
		logger.info("コンバート処理用のthreadを準備します。");
		transcoder = new Transcoder();
		transcodeThread = new Thread(transcoder);
		transcodeThread.setDaemon(true);
		transcodeThread.start();
	}
	/**
	 * 終了処理
	 */
	public void close() {
		// 強制停止
		executors.shutdownNow();
	}
	/**
	 * 関連づいている出力コンテナを再初期化する。
	 * @param audio 音声があるかフラグ
	 * @param video 映像があるかフラグ
	 */
	public void resetupOutputContainer(boolean audioFlg, boolean videoFlg) {
		logger.info("出力コンテナの準備を実行します。");
		// clearだけでいいのか？ほんとうは閉じないとだめなのでは？
		videoEncodeManagers.clear();
		audioEncodeManagers.clear();
		videoResampleManagers.clear();
		audioResampleManagers.clear();
		// TODO 2の対処:ここのセットアップ動作をマルチスレッド化しておく必要あり。
		// 以下マルチスレッドで対処する。
		// コンテナの再構築を実行する。
		List<Future<?>> futures = new ArrayList<Future<?>>();
		for(final MediaManager manager : mediaManagers) {
			futures.add(executors.submit(new Runnable() {
				@Override
				public void run() {
					manager.resetupContainer();
				}
			}));
		}
		// エンコーダーのセットアップがおわるまで待機します。
		for(Future<?> future : futures) {
			try {
				future.get();
			}
			catch (Exception e) {
				throw new RuntimeException("コンテナ再構築に失敗しました。");
			}
		}
		futures.clear();
		for(MediaManager manager : mediaManagers) {
			logger.info("mediaManagerを読み取りました。設定をはじめます。");
			if(videoFlg) {
				setupVideoEncodeManagers(manager);
			}
			if(audioFlg) {
				setupAudioEncodeManagers(manager);
			}
		}
		// エンコーダーの再構築を実行する。
		if(videoFlg) {
			for(final VideoEncodeManager videoEncodeManager : videoEncodeManagers) {
				futures.add(executors.submit(new Runnable() {
					@Override
					public void run() {
						videoEncodeManager.setupCoder();
					}
				}));
			}
		}
		if(audioFlg) {
			for(final AudioEncodeManager audioEncodeManager : audioEncodeManagers) {
				futures.add(executors.submit(new Runnable() {
					@Override
					public void run() {
						audioEncodeManager.setupCoder();
					}
				}));
				audioEncodeManager.setupCoder();
			}
		}
		for(Future<?> future : futures) {
			try {
				future.get();
			}
			catch (Exception e) {
				throw new RuntimeException("コーダー再生成に失敗しました。");
			}
		}
		// リサンプラーの構築では、coderが必要なので、コーダーが開いてから処理する。
		if(videoFlg) {
			for(VideoEncodeManager videoEncodeManager : videoEncodeManagers) {
				setupVideoResamplerManagers(videoEncodeManager);
			}
		}
		if(audioFlg) {
			for(AudioEncodeManager audioEncodeManager : audioEncodeManagers) {
				setupAudioResamplerManagers(audioEncodeManager);
			}
		}
		// containerのheaderを書き込む必要あり。
		for(MediaManager manager : mediaManagers) {
			logger.info("headerを書き込みます。{}", manager);
			manager.writeHeader();
		}
	}
	private void setupVideoResamplerManagers(VideoEncodeManager videoEncodeManager) {
		for(VideoResampleManager videoResampleManager : videoResampleManagers) {
			if(videoResampleManager.addEncodeManager(videoEncodeManager)) {
				// 登録できたので、次に移動
				return;
			}
		}
		VideoResampleManager videoResampleManager = new VideoResampleManager(videoEncodeManager);
		videoResampleManagers.add(videoResampleManager);
	}
	private void setupAudioResamplerManagers(AudioEncodeManager audioEncodeManager) {
		logger.info("音声のencodeManagerを作成します。");
		for(AudioResampleManager audioResampleManager : audioResampleManagers) {
			if(audioResampleManager.addEncodeManager(audioEncodeManager)) {
				logger.info("使い回し");
				// 登録できたので、次に移動
				return;
			}
		}
		logger.info("新規");
		AudioResampleManager audioResampleManager = new AudioResampleManager(audioEncodeManager);
		audioResampleManagers.add(audioResampleManager);
	}
	private void setupVideoEncodeManagers(MediaManager manager) {
		logger.info("映像のencodeManagerを作成します。");
		// それぞれに対してvideoCoderとaudioCoderを必要であれば開く必要あり。
		for(VideoEncodeManager videoEncodeManager : videoEncodeManagers) {
			if(videoEncodeManager.addMediaManager(manager)) {
				logger.info("使い回し");
				// 登録できたので、次にいく。
				return;
			}
		}
		logger.info("新規");
		// 対象となるvideoEncodeManagerが存在しなかったのであたらしく生成する。
		VideoEncodeManager videoEncodeManager = new VideoEncodeManager(manager);
		videoEncodeManagers.add(videoEncodeManager);
	}
	private void setupAudioEncodeManagers(MediaManager manager) {
		logger.info("音声のencodeManagerを作成します。");
		for(AudioEncodeManager audioEncodeManager : audioEncodeManagers) {
			if(audioEncodeManager.addMediaManager(manager)) {
				logger.info("使い回し");
				return;
			}
		}
		logger.info("新規");
		AudioEncodeManager audioEncodeManager = new AudioEncodeManager(manager);
		audioEncodeManagers.add(audioEncodeManager);
	}
	public void writeHeaderData() {
		
	}
	/**
	 * flvデータをサーバーから受け取ったときの動作
	 * @param flvAtom
	 */
	public void writeData(FlvAtom flvAtom) {
		ChannelBuffer buf = flvAtom.write();
		if(rawTakManager != null) {
			// そのままのデータをhttpTakStreamingにする動作がのこっている場合はそこにデータを投げるようにしておく。
		}
		// コンバート動作にflvデータをまわす。
		if(flvManager != null) {
			flvManager.writeData(buf.toByteBuffer());
//			flvManager.writeData(flvAtom);
		}
	}
	public void executeAudio(IAudioSamples decodedSamples) {
		// データをリサンプルする。
		for(AudioResampleManager audioResampleManager : audioResampleManagers) {
			// リサンプルかける。
			List<Future<?>> list = new ArrayList<Future<?>>();
			final IAudioSamples resampledData = audioResampleManager.resampleAudio(decodedSamples);
			// エンコード処理をマルチスレッド化しておく。
			for(final AudioEncodeManager audioEncodeManager : audioResampleManager.getEncodeManagers()) {
				// エンコードを実行する。
				list.add(executors.submit(new Runnable() {
					@Override
					public void run() {
						audioEncodeManager.encodeAudio(resampledData);
					}
				}));
			}
			for(Future<?> f : list) {
				try {
					f.get();
				}
				catch (Exception e) {
				}
			}
		}
	}
	public void executeVideo(IVideoPicture decodedPicture) {
		// TODO 入れ子構造にすると、中途でつまってしまうみたいです。(executorのthreadが枯渇する。)
		// fixedではなくcachedとかつかえばいいかもしれませんけど。
		// データをリサンプルする。
		for(VideoResampleManager videoResampleManager : videoResampleManagers) {
			// リサンプルをかける。
			List<Future<?>> list = new ArrayList<Future<?>>();
			final IVideoPicture resampledData = videoResampleManager.resampleVideo(decodedPicture);
			// エンコード処理をマルチスレッド化しておく。
			for(final VideoEncodeManager videoEncodeManager : videoResampleManager.getEncodeManagers()) {
				// エンコード実行
				list.add(executors.submit(new Runnable() {
					@Override
					public void run() {
						videoEncodeManager.encodeVideo(resampledData);
					}
				}));
			}
			for(Future<?> f : list) {
				try {
					f.get();
				}
				catch (Exception e) {
				}
			}
		}
	}
}
