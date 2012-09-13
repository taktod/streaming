package com.ttProject.xuggle;

import java.util.Map;

import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IPixelFormat.Type;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;

/**
 * コンバートの動作を管理するManager
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここではコーデック変換を担当
 */
public class VideoEncodeManager {
	private IStreamCoder videoCoder = null;
	public VideoEncodeManager(IContainer[] containers, ICodec.ID codec, int width, int height, Type pixelFormat, int globalQuality, int bitRate, IRational frameRate, int groupsOfPictures, Map<String, String> properties, Map<IStreamCoder.Flags, Boolean> flags, IRational timeBase) {
		ICodec outCodec = ICodec.findEncodingCodec(codec);
		if(outCodec == null) {
			throw new RuntimeException("video出力用のコーデックを取得することができませんでした。");
		}
		for(IContainer container : containers) {
			IStream outStream = null;
			if(videoCoder != null) {
				outStream = container.addNewStream(videoCoder);
			}
			else {
				outStream = container.addNewStream(codec);
			}
			if(outStream == null) {
				throw new RuntimeException("コンテナ用のストリーム作成失敗");
			}
			if(videoCoder == null) {
				IStreamCoder outCoder = outStream.getStreamCoder();
				outCoder.setCodec(codec);
				outCoder.setWidth(width);
				outCoder.setHeight(height);
				outCoder.setPixelType(pixelFormat);
				outCoder.setGlobalQuality(globalQuality);
				outCoder.setBitRate(bitRate);
				outCoder.setFrameRate(frameRate);
				outCoder.setNumPicturesInGroupOfPictures(groupsOfPictures);
				for(String key : properties.keySet()) {
					outCoder.setProperty(key, properties.get(key));
				}
				for(IStreamCoder.Flags key : flags.keySet()) {
					outCoder.setFlag(key, flags.get(key));
				}
				if(timeBase == null) {
					outCoder.setTimeBase(IRational.make(1, 1000));
				}
				else {
					outCoder.setTimeBase(timeBase);
				}
				outCoder.open(null, null);
				videoCoder = outCoder;
			}
		}
	}
}
