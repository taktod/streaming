package com.ttProject.xuggle.flv;

import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStreamCoder;

/**
 * コンバートのデータ取得処理を管理するManager
 * @author taktod
 * 基本:
 * 元ファイル -> データ取得 -> リサンプル -> コーデック変換 -> コンテナに突っ込む
 * ここでは元ファイル -> データ取得の部分を担当
 */
public class FlvManager {
	private IContainer inputContainer = null;
	private IStreamCoder inputAudioCoder = null;
	private IStreamCoder inputVideoCoder = null;
	private int audioStreamId = -1;
	private int videoStreamId = -1;
}
