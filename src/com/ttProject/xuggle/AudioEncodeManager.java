package com.ttProject.xuggle;

import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;

/**
 * コンバートの動作を管理するManager
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここではコーデック変換を担当
 * 
 * encodeManagerはaudioもしくはvideoのチャンネルが追加されたら作りなおす必要がでてくる。
 */
public class AudioEncodeManager {
	// データが追加されてしまったら、コンテナをつくりなおして、codecを作り直す必要があるらしい。
	// ということはcontainerを保持しておいてあとで必要になったら開き直すというのは意味がない。
	
	// コンテナからstreamを取り出して、streamからcoderを取り出す必要あり。
	// ということは同じコーデックでも使い回しはできないのか？
	// それとも必要があるなら使い回せるのか？そのあたり疑問
	// とりあえず情報を全く一致させてあるなら、必要ないとして組んでみる。
	private IStreamCoder audioCoder = null;
	public AudioEncodeManager(IContainer[] containers, ICodec.ID codec, int bitRate,  int sampleRate, int channels) {
		// すべてのコンテナのオーディオトラックを追加し開く。
		ICodec outCodec = ICodec.findEncodingCodec(codec);
		if(outCodec == null) {
			throw new RuntimeException("audio出力用のコーデックを取得することができませんでした。");
		}
		for(IContainer container : containers) {
			IStream outStream = null;
			if(audioCoder != null) {
				outStream = container.addNewStream(audioCoder);
			}
			else {
				outStream = container.addNewStream(codec);
			}
			if(outStream == null) {
				throw new RuntimeException("コンテナ用のストリーム作成失敗");
			}
			if(audioCoder == null) {
				IStreamCoder outCoder = outStream.getStreamCoder();
				outCoder.setBitRate(bitRate);
				outCoder.setSampleRate(sampleRate);
				outCoder.setChannels(channels);
				outCoder.open(null, null);
				audioCoder = outCoder;
			}
		}
	}
	public void encode(IAudioSamples samples) {
		// すでにaudioCoderを開いているか確認
		if(audioCoder == null) {
			throw new RuntimeException("コーダーのないコンテナの変換を実行しようとしました。");
		}
	}
}
