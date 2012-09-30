package com.ttProject.xuggle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	/**
	 * コンストラクタ
	 */
	private ConvertManager() {}
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
		// リサンプラーはどうやって決める？
		// 元データ	リサンプラーA	エンコードAA	出力AAA
		// 									出力AAB
		// 						エンコードAB	出力ABA
		// 									出力ABB
		// 			リサンプラーB	エンコードBA	出力BAA
		// 			リサンプラーC	エンコードCA	出力CAA
		// 									出力CAB
		// みたいな感じになる。
		// 出力データ一覧は別途取得可能なので、出力データ一覧から、リサンプラー一覧を構築していき。
		// 処理的には、リサンプラー郡 -> 出力データ一覧が取得可能な形にしておくべき。
		// AudioResampleManagerをつくる。(変換必須数をしらべてつくる。)
		// VideoResampleManagerをつくる。(変換必須数をしらべてつくる。)
		// AudioEncodeManagerをつくる。
		// VideoEncodeManagerをつくる。
		// 出力コンテナもつくる。
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
		for(MediaManager manager : mediaManagers) {
			logger.info("mediaManagerを読み取りました。設定をはじめます。");
			// コンテナを開き直します。
			manager.resetupContainer();
			setupVideoEncodeManagers(manager);
			setupAudioEncodeManagers(manager);
		}
		for(VideoEncodeManager videoEncodeManager : videoEncodeManagers) {
			videoEncodeManager.setupCoder();
			setupVideoResamplerManagers(videoEncodeManager);
		}
		for(AudioEncodeManager audioEncodeManager : audioEncodeManagers) {
			audioEncodeManager.setupCoder();
			setupAudioResamplerManagers(audioEncodeManager);
		}
		// containerのheaderを書き込む必要あり。
		for(MediaManager manager : mediaManagers) {
			logger.info("headerを書き込みます。");
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
		for(AudioResampleManager audioResampleManager : audioResampleManagers) {
			if(audioResampleManager.addEncodeManager(audioEncodeManager)) {
				// 登録できたので、次に移動
				return;
			}
		}
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
		if(rawTakManager != null) {
			// そのままのデータをhttpTakStreamingにする動作がのこっている場合はそこにデータを投げるようにしておく。
		}
		// コンバート動作にflvデータをまわす。
		if(flvManager != null) {
			flvManager.writeData(flvAtom);
		}
	}
	public void executeAudio(IAudioSamples decodedSamples) {
		IAudioSamples resampledData;
		// データをリサンプルする。
		for(AudioResampleManager audioResampleManager : audioResampleManagers) {
			// リサンプルかける。
			resampledData = audioResampleManager.resampleAudio(decodedSamples);
			for(AudioEncodeManager audioEncodeManager : audioResampleManager.getEncodeManagers()) {
				// エンコードを実行する。
				audioEncodeManager.encodeAudio(resampledData);
			}
		}
	}
	public void executeVideo(IVideoPicture decodedPicture) {
		IVideoPicture resampledData;
		// データをリサンプルする。
		for(VideoResampleManager videoResampleManager : videoResampleManagers) {
			// リサンプルをかける。
			resampledData = videoResampleManager.resampleVideo(decodedPicture);
			for(VideoEncodeManager videoEncodeManager : videoResampleManager.getEncodeManagers()) {
				// エンコード実行
				videoEncodeManager.encodeVideo(resampledData);
			}
		}
	}
}
