package com.ttProject.xuggle.in.red5;

import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.flv.FLVHeader;

/**
 * xuggleのIURLProtocolHandlerはffmpegが処理のトリガーになってしまっているので、次のような動作にする。
 * １：データをqueueにためておく。
 * ２：ffmpegから要求があれば、queueから必要数のデータを取り出して応答する。
 * xuggle-xuggler-red5とは違い、ここでは、このqueueにデータをいれる段階でflvのパケットに分解しておきたいと思います。(すぐにつかわれるので、たいした違いはないだろうというのと変換処理をできるだけ軽くするため、こちらのthreadで処理してしまう。)
 * 
 * 今回のrtmp -> flv変換の元ネタはred5 1.0.0rc2のorg.red5.io.flv.impl.FLVWriterとします。
 * @author toktod
 */
public class Red5DataQueue {
	/** 始めの10パケット分は、動作を放置します。 */
	private final int SKIP_COUNT = 10;
	/** スキップ中かどうか */
	private int skipCounter = 0;
	/** flvHeader */
	private FLVHeader header = null;
	private Set<ITag> metaData = null;
	/** 動画先頭タグ */ 
	private ITag videoFirstTag = null;
	/** 音声先頭タグ */
	private ITag audioFirstTag = null;
	/** コンバート中かどうかフラグ */
	private boolean onConverting = false;
	// キーフレーム判定は11バイトのデータに0x10のビットフラグがたっているかで判定する。
	/**
	 * headerデータを更新します。
	 * @param header
	 * @param metaData
	 * @param videoFirstTag
	 * @param audioFirstTag
	 */
	public void putHeaderData(FLVHeader header, Set<ITag> metaData, ITag videoFirstTag, ITag audioFirstTag) {
		// コンバート開始前のデータ待ちになっている状態の場合はflvHeaderを保持しておかないとだめ。(保持しておいて、スタートするタイミングですべて書き込む方が吉)
		// すでにコンバートが成立しているときにheaderの生成依頼がきた場合は、ffmpegのコンバートを再度実行しなおさないとだめ。
	}
	/**
	 * tagデータを更新します。
	 * @param tag
	 */
	public void putTagData(ITag tag) {
		byte dataType;
		// tagのデータから書き込みを実施する。
		if(onConverting == false) {
			// コンバート前の状態の場合はカウンターをすすめる。
			skipCounter ++;
			if(skipCounter < SKIP_COUNT) {
				// もうすこし待つ。
				return;
			}
			if(videoFirstTag != null) {
				// 映像ありの動画
				// 動画のキーフレームがくるのを待つ。
				dataType = tag.getDataType();
				if(dataType != ITag.TYPE_VIDEO) {
					return; // 動画データでないなら、動画データを待つ
				}
				IoBuffer bodyBuffer = tag.getBody().asReadOnlyBuffer();
				if((bodyBuffer.get() & 0x10) == 0x00) {
					// キーフレームでない場合
					return; // スキップ
				}
			}
			// 動作開始条件にあてはまったので、開始します。
		}
	}
}
