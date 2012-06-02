package com.ttProject.xuggle;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ttProject.xuggle.in.flv.FlvInputManager;
import com.ttProject.xuggle.out.mpegts.MpegtsOutputManager;
import com.xuggle.xuggler.IAudioResampler;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.SimpleMediaFile;

/**
 * Xuggleでメディアデータのコンバートを実行します。
 * @author taktod
 */
public class Transcoder implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(Transcoder.class);
	// 外部から指定されるもの。
	private ISimpleMediaFile outputStreamInfo;
	private Map<String, String> videoProperties = new HashMap<String, String>();
	private Map<IStreamCoder.Flags, Boolean> videoFlags = new HashMap<IStreamCoder.Flags, Boolean>();
	// 外部から指定されるもの。カスタム
	private final String inputProtocol;
	private final String inputFormat;
	private final String outputProtocol;
	private final String outputFormat;
	private final String taskName;

	// 内部で定義するもの。
	// 入力データ編
	private IContainer inputContainer = null;
	private IStreamCoder inputAudioCoder = null;
	private IStreamCoder inputVideoCoder = null;
	private int audioStreamId = -1;
	private int videoStreamId = -1;

	// 出力データ編
	private IContainer outputContainer = null;
	private IStreamCoder outputAudioCoder = null;
	private IStreamCoder outputVideoCoder = null;
	
	// リサンプラー編
	private boolean isVideoResamplerChecked = false;
	private boolean isAudioResamplerChecked = false;
	private IVideoResampler videoResampler = null;
	private IAudioResampler audioResampler = null;

	// 動作定義
	private volatile boolean keepRunning = true;
	
	/**
	 * インプット要素がなんであるか指定が必要
	 * 本来はここに、各種コンバート(jpegmp3ストリームだけか？)の指定が必要
	 */
	public Transcoder(
			FlvInputManager inputManager,
			MpegtsOutputManager outputManager,
			String name) {
		logger.info("トランスコーダーの初期化");
		// 作成するファイルについての詳細をつくりあげる必要がある。
		// コンバートのcodecを入手したときに、変換後のデータ用のコーデックとして必要なものを記述しておかないといけない。
		// TODO とりあえず一回つくってみよう。
		// 出力ファイルの設定が必要。(実際に存在するかは、あとで決める。)
		outputStreamInfo = outputManager.getStreamInfo();
		videoProperties.putAll(outputManager.getVideoProperty());
		videoFlags.putAll(outputManager.getVideoFlags());
		
		inputProtocol = inputManager.getProtocol();
		inputFormat = inputManager.getFormat();
		outputProtocol = outputManager.getProtocol();
		outputFormat = outputManager.getFormat();
		taskName = name;
	}
	@Override
	public void run() {
		try {
			logger.info("transcoderを起動しました。");
			// 読み込み用のコンテナをオープン
			openInputContainer();
			// 変換を実行
			transcode();
			// 終わったら変換を止める。
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			closeAll();
		}
	}
	/**
	 * 停止します。
	 */
	public void close() {
		keepRunning = false;
	}
	/**
	 * 必要のないオブジェクトの解放を実施します。
	 */
	private void closeAll() {
		try {
			closeOutputContainer();
			closeInputContainer();
		}
		finally {
//			notifyAll();
		}
	}
	/**
	 * 読み込み用のコンテナを開く
	 */
	private void openInputContainer() {
		String url;
		int retval = -1;
		logger.info("読み込みコンテナを開きます。");
		url = inputProtocol + ":" + taskName;
		ISimpleMediaFile inputInfo = new SimpleMediaFile();
		inputInfo.setURL(url);
		inputContainer = IContainer.make();
		IContainerFormat inputFormat = IContainerFormat.make();
		inputFormat.setInputFormat(this.inputFormat); // 形式をflvにしておく。
		retval = inputContainer.open(url, IContainer.Type.READ, inputFormat, true, false);
		if(retval < 0) {
			throw new RuntimeException("入力用のURLを開くことができませんでした。" + url);
		}
	}
	/**
	 * 変換を実行
	 */
	private void transcode() {
		int retval = -1;
		// 動作パケットの受け皿を準備しておく。
		IPacket packet = IPacket.make();
		while(keepRunning) {
			// パケットの入力を取得する。
			retval = inputContainer.readNextPacket(packet);
			if(retval < 0) {
				// もしかしたら、特定のFLVではここにくることがあるかもしれない。
				// データの受け取りに失敗したので、おわる。
				keepRunning = false;
				break;
			}
			IPacket decodePacket = packet;
			// 入力コーダーを開きます。
			if(!checkInputCoder(decodePacket)) {
				// 処理する必要のないパケットなのでスキップします。
				continue;
			}
			// デコードします。
			int index = decodePacket.getStreamIndex();
			if(index == audioStreamId) {
				// 音声を処理します。
				executeAudio(decodePacket);
			}
			else if(index == videoStreamId) {
				executeVideo(decodePacket);
			}
		}
	}
	private void closeInputContainer() {
		if(inputVideoCoder != null) {
			inputVideoCoder.close();
			inputVideoCoder = null;
		}
		if(inputAudioCoder != null) {
			inputAudioCoder.close();
			inputAudioCoder = null;
		}
		if(inputContainer != null) {
			inputContainer.close();
		}
	}
	private void closeOutputContainer() {
		if(outputContainer != null) {
			outputContainer.writeTrailer();
		}
		if(outputVideoCoder != null) {
			outputVideoCoder.close();
			outputVideoCoder = null;
		}
		if(outputAudioCoder != null) {
			outputAudioCoder.close();
			outputAudioCoder = null;
		}
		if(outputContainer != null) {
			outputContainer.close();
			outputContainer = null;
		}
	}
	/**
	 * 出力用のコンテナを開く
	 */
	private void openOutputContainer() {
		String url;
		int retval = -1;
		logger.info("書き込みコンテナを開きます。");
		url = outputProtocol + ":" + taskName;
		ISimpleMediaFile outputInfo = new SimpleMediaFile();
		outputInfo.setURL(url);
		outputContainer = IContainer.make();
		IContainerFormat outputFormat = IContainerFormat.make();
		outputFormat.setOutputFormat(this.outputFormat, url, null);
		retval = outputContainer.open(url, IContainer.Type.WRITE, outputFormat);
		if(retval < 0) {
			throw new RuntimeException("出力用のURLを開くことができませんでした。" + url);
		}
		if(videoStreamId != -1) {
			// videoストリームを開く
			openOutputVideoCoder();
		}
		if(audioStreamId != -1) {
			// audioストリームを開く
			openOutputAudioCoder();
		}
		retval = outputContainer.writeHeader();
		if(retval < 0) {
			throw new RuntimeException("出力ヘッダの書き込みに失敗しました。");
		}
	}
	private void openOutputVideoCoder() {
		IStream outStream = outputContainer.addNewStream(outputContainer.getNumStreams());
		if(outStream == null) {
			throw new RuntimeException("video出力用のストリーム生成ができませんでした。");
		}
		IStreamCoder outCoder = outStream.getStreamCoder();
		ICodec outCodec = ICodec.findEncodingCodec(outputStreamInfo.getVideoCodec());
		if(outCodec == null) {
			throw new RuntimeException("video出力用のエンコードコーデックを取得することができませんでした。");
		}
		outCoder.setCodec(outCodec);
		outCoder.setWidth(outputStreamInfo.getVideoWidth());
		outCoder.setHeight(outputStreamInfo.getVideoHeight());
		outCoder.setPixelType(outputStreamInfo.getVideoPixelFormat());
		outCoder.setGlobalQuality(outputStreamInfo.getVideoGlobalQuality());
		outCoder.setBitRate(outputStreamInfo.getVideoBitRate());
		outCoder.setFrameRate(outputStreamInfo.getVideoFrameRate());
		outCoder.setNumPicturesInGroupOfPictures(outputStreamInfo.getVideoNumPicturesInGroupOfPictures());
		// 細かいプロパティ
		for(String key : videoProperties.keySet()) {
			outCoder.setProperty(key, videoProperties.get(key));
		}
		// flags
		for(IStreamCoder.Flags key : videoFlags.keySet()) {
			outCoder.setFlag(key, videoFlags.get(key));
		}
		if(outputStreamInfo.getVideoTimeBase() != null) {
			outCoder.setTimeBase(outputStreamInfo.getVideoTimeBase());
		}
		else {
			outCoder.setTimeBase(IRational.make(1, 1000));
		}
		if(outCoder.open() < 0) {
			throw new RuntimeException("出力コーダーをオープンするのに失敗しました。");
		}
		// 開くことに成功したので以降これを利用する。
		outputVideoCoder = outCoder;
	}
	private void openOutputAudioCoder() {
		IStream outStream = outputContainer.addNewStream(outputContainer.getNumStreams());
		if(outStream == null) {
			throw new RuntimeException("audio出力用のストリーム生成ができませんでした。");
		}
		IStreamCoder outCoder = outStream.getStreamCoder();
		ICodec outCodec = ICodec.findEncodingCodec(outputStreamInfo.getAudioCodec());
		if(outCodec == null) {
			throw new RuntimeException("audio出力用のエンコーダーを取得することができませんでした。");
		}
		outCoder.setCodec(outCodec);
		outCoder.setBitRate(outputStreamInfo.getAudioBitRate());
		outCoder.setSampleRate(outputStreamInfo.getAudioSampleRate());
		outCoder.setChannels(outputStreamInfo.getAudioChannels());
		outCoder.open();
		// 開くことに成功したので以降これを利用する。
		outputAudioCoder = outCoder;
	}
	/**
	 * パケット情報から、動作に必要なコーダーを開きます。
	 * また映像or音声のあたらしいパケットを入手した場合は出力ファイルを変更する必要がでてくるので、そっちの処理も実行します。
	 * とりあえず始めはaddNewStreamがあるので、そっちでできることを期待します。
	 * @param packet
	 */
	private boolean checkInputCoder(IPacket packet) {
		// どうやらContainerにaddNewStreamをしない限り、動作できるらしい。(あとから追加が可能？っぽい。)
		IStream stream = inputContainer.getStream(packet.getStreamIndex());
		if(stream == null) {
			// ストリームが取得できませんでした。
			return false;
		}
		IStreamCoder coder = stream.getStreamCoder();
		if(coder == null) {
			// coderが取得できませんでした。
			return false;
		}
		if(coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
			
			// audioCodec
			if(inputAudioCoder == null) {
				// audioの入力Coderが設定されていない場合は、はじめてのアクセスなので、処理する必要がある。
				if(!outputStreamInfo.hasAudio()) {
					// audio出力が必要ない場合は処理しない。
					return false;
				}
				audioStreamId = packet.getStreamIndex();
				// 必要があるなら、出力コンテナーを閉じる
				closeOutputContainer();
				// 出力コンテナーを生成する。
				openOutputContainer();
			}
			else if(inputAudioCoder.hashCode() == coder.hashCode()) {
				// コーダーが一致する場合はこのままコーダーをつかって処理すればよい。
				return true;
			}
			else {
				inputAudioCoder.close();
				inputAudioCoder = null;
				audioStreamId = packet.getStreamIndex();
			}
			// 入力Audioコーダーとリサンプラーを準備しておく。
			if(coder.open() < 0) {
				throw new RuntimeException("audio入力用のデコーダーを開くのに失敗しました。");
			}
			isAudioResamplerChecked = false;
			inputAudioCoder = coder;
		}
		else if(coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
			if(inputVideoCoder == null) {
				if(!outputStreamInfo.hasVideo()) {
					return false;
				}
				videoStreamId = packet.getStreamIndex();
				// 必要があるなら、出力コンテナーを閉じる
				closeOutputContainer();
				// 出力コンテナーを生成する。
				openOutputContainer();
			}
			else if(inputVideoCoder.hashCode() == coder.hashCode()){
				// コーダーが一致する場合はこのままコーダーをつかって処理をすればよい。
				return true;
			}
			else {
				inputVideoCoder.close();
				inputVideoCoder = null;
				videoStreamId = packet.getStreamIndex();
			}
			// 入力Videoコーダーを準備しておく。
			if(coder.open() < 0) {
				throw new RuntimeException("video入力用のデコーダーを開くのに失敗しました。");
			}
			isVideoResamplerChecked = false;
			inputVideoCoder = coder;
		}
		else {
			// 音声でも映像でもないものは処理しない。
			return false;
		}
		return true;
	}
	/**
	 * 音声のリサンプラーを確認します。
	 * @param samples
	 */
	private void checkAudioResampler(IAudioSamples samples) {
		if(isAudioResamplerChecked) {
			// すでにリサンプラが確認済みの場合は処理しない。
			return;
		}
		audioResampler = null;
		isAudioResamplerChecked = true;
		// リサンプラーの準備ができていないので、必要があるなら開く
		if(outputAudioCoder.getSampleRate() == samples.getSampleRate()
				&& outputAudioCoder.getChannels() == samples.getChannels()) {
			// サンプリングレートとチャンネル数が一致する場合は変換なしで動作するので、そのまま進める。
			return;
		}
		audioResampler = IAudioResampler.make(outputAudioCoder.getChannels(), samples.getChannels(), outputAudioCoder.getSampleRate(), samples.getSampleRate());
		if(audioResampler == null) {
			throw new RuntimeException("audioリサンプラーが開けませんでした。");
		}
	}
	/**
	 * 映像のリサンプラーを確認します。
	 * @param picture
	 */
	private void checkVideoResampler(IVideoPicture picture) {
		if(isVideoResamplerChecked) {
			// 既にビデオのデータは確認済みなので、処理する必要なし。
			return;
		}
		videoResampler = null;
		isVideoResamplerChecked = true;
		if(outputVideoCoder.getPixelType() == picture.getPixelType()
				&& outputVideoCoder.getWidth() == picture.getWidth()
				&& outputVideoCoder.getHeight() == picture.getHeight()) {
			// サイズとピクセルタイプが一致する場合は変換する必要なし。
			return;
		}
		videoResampler = IVideoResampler.make(outputVideoCoder.getWidth(), outputVideoCoder.getHeight(), outputVideoCoder.getPixelType(), picture.getWidth(), picture.getHeight(), picture.getPixelType());
		if(videoResampler == null) {
			throw new RuntimeException("videoリサンプラーを開くのに失敗しました。");
		}
	}
	/**
	 * 音声データをデコードします。
	 * @param targetPacket
	 */
	private void executeAudio(IPacket targetPacket) {
		int retval = -1;
		IAudioSamples inSamples = IAudioSamples.make(1024, inputAudioCoder.getChannels());
		IAudioSamples reSamples = null;
		int offset = 0;
		while(offset < targetPacket.getSize()) {
			retval = inputAudioCoder.decodeAudio(inSamples, targetPacket, offset);
			if(retval <= 0) {
				throw new RuntimeException("audioのデコードに失敗しました。");
			}
			offset += retval;
			
			IAudioSamples postDecode = inSamples;
			if(postDecode.isComplete()) {
				reSamples = resampleAudio(postDecode);
				
				if(reSamples.isComplete()) {
					// エンコードを実施します。
					encodeAudio(reSamples);
				}
			}
		}
	}
	/**
	 * 音声データをリサンプルします。入力と出力の形式が違うときに合わせます。
	 * @param samples
	 * @return
	 */
	private IAudioSamples resampleAudio(IAudioSamples samples) {
		// リサンプラーが必要か確認
		checkAudioResampler(samples);
		
		if(samples == null) {
			return samples;
		}
		IAudioSamples outSamples = IAudioSamples.make(1024, outputAudioCoder.getChannels());
		IAudioSamples preResample = samples;
		int retval = -1;
		retval = audioResampler.resample(outSamples, preResample, preResample.getNumSamples());
		if(retval < 0) {
			throw new RuntimeException("audioのリサンプルに失敗しました。");
		}
		IAudioSamples postResample = outSamples;
		return postResample;
	}
	/**
	 * オーディオデータをエンコードします。
	 * @param samples
	 */
	private void encodeAudio(IAudioSamples samples) {
		int retval = -1;
		IPacket outPacket = IPacket.make();
		
		IAudioSamples preEncode = samples;
		
		int numSamplesConsumed = 0;
		while(numSamplesConsumed < preEncode.getNumSamples()) {
			retval = outputAudioCoder.encodeAudio(outPacket, preEncode, numSamplesConsumed);
			if(retval <= 0) {
//				throw new RuntimeException("audioをエンコードすることができませんでした。");
				System.out.println("audioエンコードに失敗しましたが、このまま続けます。");
				break;
			}
			else {
				System.out.println("audioエンコードに成功しました。");
			}
			numSamplesConsumed += retval;
			
			if(outPacket.isComplete()) {
				// ここで出力ファイルができあがる。
//				outputContainer.writePacket(outPacket);
			}
		}
	}
	/**
	 * 映像データを処理します。
	 * @param targetPacket
	 */
	private void executeVideo(IPacket targetPacket) {
		int retval = -1;
		IVideoPicture inPicture = IVideoPicture.make(inputVideoCoder.getPixelType(), inputVideoCoder.getWidth(), inputVideoCoder.getHeight());
		
		IVideoPicture reSample = null;
		int offset = 0;
		while(offset < targetPacket.getSize()) {
			retval = inputVideoCoder.decodeVideo(inPicture, targetPacket, offset);
			if(retval <= 0) {
				logger.error(IError.make(retval).getDescription());
				throw new RuntimeException("Videoパケットをデコードすることができませんでした。");
			}
			offset += retval;
			
			IVideoPicture postDecode = inPicture;
			if(postDecode.isComplete()) {
				reSample = resampleVideo(postDecode);
				
				if(reSample.isComplete()) {
					// エンコードを実行します。
					encodeVideo(reSample);
				}
			}
		}
	}
	/**
	 * 映像をリサンプルします。
	 * @param picture
	 * @return
	 */
	private IVideoPicture resampleVideo(IVideoPicture picture) {
		checkVideoResampler(picture);
		if(videoResampler == null) {
			// リサンプルする必要なし。
			return picture;
		}
		IVideoPicture outPicture = IVideoPicture.make(outputVideoCoder.getPixelType(), outputVideoCoder.getWidth(), outputVideoCoder.getHeight());
		
		IVideoPicture preResample = picture;
		int retval = -1;
		retval = videoResampler.resample(outPicture, preResample);
		if(retval < 0) {
			throw new RuntimeException("videoのリサンプルに失敗しました。");
		}
		IVideoPicture postResample = outPicture;
		return postResample;
	}
	/**
	 * 映像をエンコードします。
	 * @param picture
	 */
	private void encodeVideo(IVideoPicture picture) {
		int retval = -1;
		IPacket outPacket = IPacket.make();
		
		IVideoPicture preEncode = picture;
		int numBytesConsumed = 0;
		if(preEncode.isComplete()) {
			retval = outputVideoCoder.encodeVideo(outPacket, preEncode, 0);
			if(retval <= 0) {
				System.out.println("videoのエンコードに失敗しましたが、そのまま続けます。");
			}
			else {
				System.out.println("videoのエンコードに成功");
				numBytesConsumed += retval;
			}
			if(outPacket.isComplete()) {
//				outputContainer.writePacket(outPacket);
			}
		}
	}
}
