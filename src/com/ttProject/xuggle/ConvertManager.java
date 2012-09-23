package com.ttProject.xuggle;

import java.util.List;

import com.flazr.io.flv.FlvAtom;
import com.ttProject.streaming.MediaManager;
import com.ttProject.streaming.tak.TakManager;
import com.ttProject.xuggle.flv.FlvManager;

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
//	private static final Map<String, ConvertManager> instances = new ConcurrentHashMap<String, ConvertManager>();
	private static final ConvertManager instance = new ConvertManager();
	/** 名前保持 */
	private String name;
	public String getName() {
		return name;
	}
	/** 生データのtakStreaming用のmanager */
	private TakManager rawTakManager = null;
	/** コンバート用のflvManager */
	private FlvManager flvManager = null;
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
		// 名前を保持しておく。
		this.name = name;
		// encode.xmlから必要な情報を抜き出しておく。
		EncodeXmlAnalizer analizer = EncodeXmlAnalizer.getInstance();
		// このマネージャーリストにそって、コンバートを実行する必要がある。
		List<MediaManager> mediaManagers = analizer.getManagers(); // 作成された、MediaManagerリストを取得する
		
		boolean needConvert = false;
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
				else {
					needConvert = true;
				}
			}
			else {
				needConvert = true;
			}
		}
		if(!needConvert) {
			// コンバートする必要がないので、ここでおわる。
			return;
		}
		// コンバートを実行する必要がある。
		// FlvManagerをつくる。
		flvManager = new FlvManager();
		// AudioResampleManagerをつくる。(変換必須数をしらべてつくる。)
		// VideoResampleManagerをつくる。(変換必須数をしらべてつくる。)
		// AudioEncodeManagerをつくる。
		// VideoEncodeManagerをつくる。
		// 出力コンテナもつくる。
		// この時点で入力変換用のthreadをつくる必要がある。
		// 要するにTranscoderの部分
		// 実際の内部の初期化は、inputContainerを開く部分は入力変換用のthereadの冒頭
		// 出力コンテナの部分は、入力メディアデータのうち対象のデータをうけとった瞬間に実行(もしくは再構築)する。
	}
	/**
	 * 関連づいている出力コンテナを再初期化する。
	 * @param audio 音声があるかフラグ
	 * @param video 映像があるかフラグ
	 */
	public void resetupOutputContainer(boolean audioFlg, boolean videoFlg) {
		
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
}
