package com.ttProject.xuggle;

import java.nio.ByteBuffer;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.VideoData;
import org.slf4j.Logger;

import com.ttProject.streaming.jpegmp3.JpegModel;
import com.ttProject.streaming.jpegmp3.Mp3M3u8Model;
import com.ttProject.xuggle.red5.IRTMPEventIOHandler;
import com.ttProject.xuggle.red5.Red5HandlerFactory;
import com.ttProject.xuggle.red5.Red5Message;
import com.ttProject.xuggle.red5.Red5StreamingQueue;

import com.xuggle.xuggler.IAudioResampler;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.SimpleMediaFile;

/**
 * 実際のエンコード変換を実行します。
 * 元ネタはxuggle-xuggler-red5のtranscoder
 * TODO ちょっとこのクラスRed5と癒着しすぎなので、切り離すことを考える。
 * streamListenerによるデータのpush動作を外部にもっていく感じかな？
 * @author taktod
 */
public class Transcoder implements Runnable {
	final private Logger log = Red5LoggerFactory.getLogger(this.getClass());

	final static private Red5HandlerFactory mFactory = Red5HandlerFactory.getFactory();

	private final IBroadcastStream mInputStream;
	private final Red5StreamingQueue mInputQueue;
	private final IStreamListener mInputListener;
	private final ISimpleMediaFile mOutputInfo;
	private final IRTMPEventIOHandler mOutputHandler; // これは特には必要ないが、ないとコンバート実行できないみたいなので、とりあえずいれておく。

	// この部分で位置情報をハンドリングする。
	private static int position = -1;
	private static int basePosition = -1;
	public static void setPosition(int pos){
		if(basePosition == -1) {
			basePosition = pos;
			position = 0;
			return;
		}
		position = pos - basePosition;
		return;
	}

	private volatile boolean mIsRunning=false;
	private volatile boolean mKeepRunning=true;

	private String mInputURL;
	private String mOutputURL;
	private IContainer mOutContainer;
	private IStreamCoder mOutAudioCoder;
	private IStreamCoder mOutVideoCoder;
	private IContainer mInContainer;
	private IStreamCoder mInAudioCoder;
	private IStreamCoder mInVideoCoder;
	private int mAudioStreamId;
	private IAudioResampler mAudioResampler;
	private IVideoResampler mVideoResampler;
	private int mVideoStreamId;

	/**
	 * Create a new transcoder object.
	 * 
	 * All listeners are set to null.
	 *  
	 * @param aInputStream The stream to get input packets from.
	 * @param aOutputStream The stream to publish output packets to.
	 * @param aOutputInfo Meta data about what type of packets you want to publish.
	 * @param aPacketListener A packet listener that will be called for interesting events.  Or null to disable.
	 * @param aSamplesListener A Audio Samples listener that will be called for interesting events.  Or null to disable.
	 * @param aPictureListener A Video Picture listener that will be called for interesting events.  Or null to disable.
	 */
	public Transcoder(
			IBroadcastStream aInputStream,
			ISimpleMediaFile aOutputInfo)
	{
		if (aInputStream==null)
			throw new IllegalArgumentException("must pass input stream");
		if (aOutputInfo == null)
			throw new IllegalArgumentException("must pass output stream info");

		mInputStream = aInputStream;
		mOutputInfo = aOutputInfo;
		mInputQueue = new Red5StreamingQueue();

		mAudioStreamId = -1;
		mVideoStreamId = -1;

		// Check that we have valid input and output formats if specified
		// mpegtsにするから必要ないかも。
		if (mOutputInfo.getContainerFormat() != null)
		{
			IContainerFormat fmt = mOutputInfo.getContainerFormat();
			if (fmt.getInputFormatShortName() != "flv")
				throw new IllegalArgumentException("currently we only support inputs from FLV files");
		}
		// Make sure if specifying audio that we have all required parameters set.
		// ここのチェックも別途やっておけばいいだけの話
		if (mOutputInfo.hasAudio())
		{
			if (!mOutputInfo.isAudioBitRateKnown() || mOutputInfo.getAudioBitRate() <= 0)
				throw new IllegalArgumentException("must set audio bit rate when outputting audio");
			if (!mOutputInfo.isAudioChannelsKnown() || mOutputInfo.getAudioChannels() <= 0)
				throw new IllegalArgumentException("must set audio channels when outputting audio");
			if (!mOutputInfo.isAudioSampleRateKnown() || mOutputInfo.getAudioSampleRate() <= 0)
				throw new IllegalArgumentException("must set audio sample rate when outputting audio");
			if (mOutputInfo.getAudioCodec() == ICodec.ID.CODEC_ID_NONE)
				throw new IllegalArgumentException("must set audio code when outputting audio");
		}
		if (mOutputInfo.hasVideo())
		{
			if (!mOutputInfo.isVideoBitRateKnown() || mOutputInfo.getVideoBitRate() <= 0)
				throw new IllegalArgumentException("must set video bit rate when outputting video");
			if (!mOutputInfo.isVideoHeightKnown() || mOutputInfo.getVideoHeight() <= 0)
				throw new IllegalArgumentException("must set video height when outputting video");
			if (!mOutputInfo.isVideoWidthKnown() || mOutputInfo.getVideoWidth() <= 0)
				throw new IllegalArgumentException("must set video width when outputting video");
			if (mOutputInfo.getVideoCodec() == ICodec.ID.CODEC_ID_NONE)
				throw new IllegalArgumentException("must set video codec when outputting video");
			if (!IVideoResampler.isSupported(IVideoResampler.Feature.FEATURE_IMAGERESCALING))
				log.warn("Your installed version of AAFFMPEG doesn't support video resampling; Transcoding will fail if resizing is required");
		}
		if (!(mOutputInfo.hasAudio() || mOutputInfo.hasVideo()))
			throw new IllegalArgumentException("must output either audio or video");

		// この処理は本来そとにもっていくもの。
		mInputListener = new IStreamListener() {
			public void packetReceived(IBroadcastStream aStream, IStreamPacket aPacket) {
				try {
					IoBuffer buf = aPacket.getData();
					if (buf != null) {
						buf.rewind();
					}
					if (buf==null || buf.remaining()==0) {
						log.debug("skipping empty packet with no data");
						return;
					}

					if (aPacket instanceof AudioData) {
						log.debug("  adding packet type: {}; ts: {}; on stream: {}",
								new Object[]{"AUDIO", aPacket.getTimestamp(), aStream.getPublishedName()});
						mInputQueue.put(new Red5Message(Red5Message.Type.AUDIO, (AudioData)aPacket));
					}
					else if (aPacket instanceof VideoData) {
						Red5Message.Type type = Red5Message.Type.INTERFRAME;
						VideoData dataPacket = (VideoData)aPacket;
						switch(dataPacket.getFrameType()) {
						case DISPOSABLE_INTERFRAME:
							type = Red5Message.Type.DISPOSABLE_INTERFRAME;
							break;
						case INTERFRAME:
							type = Red5Message.Type.INTERFRAME;
							break;
						case KEYFRAME:
						case UNKNOWN:
							type = Red5Message.Type.KEY_FRAME;
							break;
						}
						if (type != Red5Message.Type.DISPOSABLE_INTERFRAME) // The FFMPEG FLV decoder doesn't handle disposable frames
						{
							log.debug("  adding packet type: {}; ts: {}; on stream: {}",
									new Object[]{dataPacket.getFrameType(), aPacket.getTimestamp(), aStream.getPublishedName()});
							mInputQueue.put(new Red5Message(type, dataPacket));
						}
					}
					else if (aPacket instanceof IRTMPEvent) {
						log.debug("  adding packet type: {}; ts: {}; on stream: {}",
								new Object[]{"OTHER", aPacket.getTimestamp(), aStream.getPublishedName()});
						Red5Message.Type type = Red5Message.Type.OTHER;
						IRTMPEvent dataPacket = (IRTMPEvent)aPacket;
						mInputQueue.put(new Red5Message(type, dataPacket));
					}
					else {
						log.debug("dropping packet type: {}; ts: {}; on stream: {}",
								new Object[]{"UNKNOWN", aPacket.getTimestamp(), aStream.getPublishedName()});
					}
				}
				catch (InterruptedException ex) {
					log.error("exception: {}", ex);
				}
			}
		};
		mOutputHandler = new IRTMPEventIOHandler() {
			// output用の処理なので、readはなにもしません。
			public Red5Message read() throws InterruptedException {
				return null;
			}
			// 書き込み処理ですが、特にすることないので(tsファイルでhookするし、別途ファイル書き込みするのでなにもしません。)
			public void write(Red5Message aMsg) throws InterruptedException {
			}
		};
	}

	/**
	 * Is the main loop running?
	 * 
	 * @see #run()
	 * @return true if the loop is running, false otherwise.
	 */
	public boolean isRunning() {
		return mIsRunning;
	}

	/**
	 * Stop the {@link Transcoder} loop if it's running
	 * on a separate thread.
	 * <p>
	 * It does this by sending a
	 * {@link Red5Message} for the end of stream 
	 * 
	 * to the {@link Transcoder} and allowing it to
	 * exit gracefully.
	 * </p>
	 * @see #run()
	 */
	public void stop() {
		try {
			mInputQueue.put(new Red5Message(Red5Message.Type.END_STREAM, null));
		}
		catch (InterruptedException e) {
			log.error("exception: {}", e);
		}
		mKeepRunning = false;
	}

	/**
	 * Open up all input and ouput containers (files)
	 * and being transcoding.
	 * <p>
	 * The {@link Transcoder} requires its own thread to
	 * do work on, and callers are responsible for
	 * allocating the {@link Thread}.
	 * </p>
	 * <p>
	 * This method does not return unless another thread
	 * calls {@link Transcoder#stop()}, or it reaches
	 * the end of a Red5 stream.  It is meant to
	 * be passed as the {@link Runnable#run()} method
	 * for a thread.
	 * </p>
	 */
	public void run() {
		try {
			// コンテナのオープン(仮に必要があればコンテナーの開き直しが必要になる。コーデック変換や設定変更があった場合等とりあえず画像サイズ変更くらいでは必要なかった。)
			openContainer();
			// 実変換処理、デコード、リサンプリング、エンコードの３つの処理を実行して、コンテナに書き込みを実施する。
			transcode();
		}
		catch (Throwable e) {
			log.error("uncaught exception: " + e.getMessage());
			e.printStackTrace();
		}
		finally {
			closeContainer();
		}
	}

	private void transcode() {
		int retval = -1;
		synchronized(this) {
			mIsRunning = true;
			notifyAll();
		}
		IPacket iPacket = IPacket.make();
		log.debug("Packets and Audio buffers created");
		while (mKeepRunning) {
			retval = mInContainer.readNextPacket(iPacket);
			if (retval <0) {
				log.debug("container is empty; exiting transcoding thread");
				mKeepRunning = false;
				break;
			}
			log.debug("next packet read");
			IPacket decodePacket = iPacket;
			openInputCoders(decodePacket); // reopen the input coders if we need to
			int i = decodePacket.getStreamIndex();
			if (i == mAudioStreamId) {
				log.debug("audio stream id matches: {}", i);
				if (mInAudioCoder == null) {
					throw new RuntimeException("audio coder not set up");
				}

				if (mOutputInfo.hasAudio()) {
					decodeAudio(decodePacket);
				}
				else {
					log.debug("dropping audio because output has no audio");
				}
			}
			else if (i == mVideoStreamId) {
				log.debug("video stream id matches: {}", i);
				if (mInVideoCoder == null) {
					throw new RuntimeException("video coder not set up");
				}

				if (mOutputInfo.hasVideo()) {
					decodeVideo(decodePacket);
				}
				else {
					log.debug("dropping video because output has no video");
				}
			}
			else {
				log.debug("dropping packet from stream we haven't set-up: {}", i);
			}
		}
	}

	private void openContainer() {
		// set out thread name
		String threadName = "Transcoder["+mInputStream.getPublishedName()+"]";
		log.debug("Changing thread name: {}; to {};", Thread.currentThread().getName(), threadName);
		Thread.currentThread().setName(threadName);
		int retval = -1;

		// First let's setup our input URL
		{
			// Register a new listener; should hopefully start getting audio packets immediately
			log.debug("Adding packet listener to stream: {}", mInputStream.getPublishedName());
			mInputStream.addStreamListener(mInputListener);

			// Tell AAFFMPEG about our new input URL; we use the unique Red5 names for the url
			mInputURL = Red5HandlerFactory.DEFAULT_PROTOCOL+":test";
			ISimpleMediaFile inputInfo = new SimpleMediaFile();
			inputInfo.setURL(mInputURL);
			mFactory.registerStream(mInputQueue, inputInfo);

			mInContainer = IContainer.make();
			// NOTE: This will block until we get the later of the first audio if it has audio, or first video
			// if it has video
			log.debug("About to open input url: {}", mInputURL);
			IContainerFormat inFormat = IContainerFormat.make();
			inFormat.setInputFormat("flv"); // set the input format to avoid FFMPEG probing
			retval = mInContainer.open(mInputURL, IContainer.Type.READ, inFormat, true, false);
			if (retval < 0) {
				throw new RuntimeException("Could not open input: " + mInputURL);
			}
		}
		// Now, let's first set up our output URL
		{
			// Tell AAFFMPEG about out output URL
			// TODO ここなんとかしておかなきゃいかん。
//		        mOutputURL = Red5HandlerFactory.DEFAULT_PROTOCOL+":"+mOutputStream.getName();
			mOutputURL = Red5HandlerFactory.DEFAULT_PROTOCOL+":"+"test"; // テストという名前固定にしてしまったが変更する必要あり。ストリーム名でいいか？
			mOutputInfo.setURL(mOutputURL); // red5の動作のままにしているのは、red5Handlerでtsファイルの実体をうけとって処理したいため。本来のプログラムとしてはおかしい。
			// For the output URL, every time we get a packet we just dispatch it to
			// a stream; you also use a Red5StreamingQueue here if you wanted to
			// have another thread deal with broadcasting.
			mFactory.registerStream(mOutputHandler, mOutputInfo);

			mOutContainer = IContainer.make();
			IContainerFormat outFormat = IContainerFormat.make();
			outFormat.setOutputFormat("mpegts", mOutputURL, null); // 扱うコンテナをtsに指定
//			outFormat.setOutputFormat("flv", mOutputURL, null);
			retval = mOutContainer.open(mOutputURL, IContainer.Type.WRITE, outFormat);
			if (retval < 0) {
				throw new RuntimeException("could not open output: "+mOutputURL);
			}

			if (mOutputInfo.hasAudio()) {
				// Add an audio stream
				IStream outStream = mOutContainer.addNewStream(0);
				if (outStream == null) {
					throw new RuntimeException("could not add audio stream to output: "+mOutputURL);
				}
				IStreamCoder outCoder = outStream.getStreamCoder();
				ICodec.ID outCodecId = mOutputInfo.getAudioCodec();
				ICodec outCodec = ICodec.findEncodingCodec(outCodecId);
				if (outCodec == null) {
					log.error("Could not encode using the codec: {}", mOutputInfo.getAudioCodec());
					throw new RuntimeException("Could not encode using the codec: "+mOutputInfo.getAudioCodec());
				}
				outCoder.setCodec(outCodec);
				outCoder.setBitRate(mOutputInfo.getAudioBitRate());
				outCoder.setSampleRate(mOutputInfo.getAudioSampleRate());
				outCoder.setChannels(mOutputInfo.getAudioChannels());
				outCoder.open();
				// if we get here w/o an exception, record the coder
				mOutAudioCoder = outCoder;
			}
			if (mOutputInfo.hasVideo()) {
				// Add an audio stream
				IStream outStream = mOutContainer.addNewStream(1);
				if (outStream == null) {
					throw new RuntimeException("could not add video stream to output: "+mOutputURL);
				}
				IStreamCoder outCoder = outStream.getStreamCoder();
				ICodec.ID outCodecId = mOutputInfo.getVideoCodec();
				ICodec outCodec = ICodec.findEncodingCodec(outCodecId);
				if (outCodec == null) {
					log.error("Could not encode using the codec: {}", mOutputInfo.getAudioCodec());
					throw new RuntimeException("Could not encode using the codec: "+mOutputInfo.getAudioCodec());
				}
				outCoder.setCodec(outCodec);
				outCoder.setWidth(mOutputInfo.getVideoWidth());
				outCoder.setHeight(mOutputInfo.getVideoHeight());
				outCoder.setPixelType(mOutputInfo.getVideoPixelFormat());
				outCoder.setGlobalQuality(mOutputInfo.getVideoGlobalQuality());
				outCoder.setBitRate(mOutputInfo.getVideoBitRate());
				outCoder.setFrameRate(mOutputInfo.getVideoFrameRate());
				outCoder.setNumPicturesInGroupOfPictures(mOutputInfo.getVideoNumPicturesInGroupOfPictures());
				// ローカル環境での動作
/*				outCoder.setProperty("coder", "1");
//				outCoder.setProperty("flags", "+loop");
				outCoder.setProperty("cmp", "+chroma");
				outCoder.setProperty("partitions", "+parti8x8+parti4x4+partp8x8+partb8x8");
				outCoder.setProperty("me_method", "hex");
				outCoder.setProperty("subq", "7");
				outCoder.setProperty("me_range", "16");
				outCoder.setProperty("g", "250");
				outCoder.setProperty("keyint_min", "25");
				outCoder.setProperty("sc_threshold", "40");
				outCoder.setProperty("i_qfactor", "0.71");
				outCoder.setProperty("b_strategy", "1");
				outCoder.setProperty("qcomp", "0.6");
				outCoder.setProperty("qmin", "10");
				outCoder.setProperty("qmax", "21");
				outCoder.setProperty("qdiff", "4");
				outCoder.setProperty("bf", "3");
				outCoder.setProperty("refs", "3");
				outCoder.setProperty("directpred", "1");
				outCoder.setProperty("trellis", "1");
//				outCoder.setProperty("flags2", "+mixed_refs+wpred+dct8x8+fastpskip+mbtree");
				outCoder.setProperty("wpredp", "2");
				outCoder.setFlag(IStreamCoder.Flags.FLAG_LOOP_FILTER, true);
				outCoder.setFlag(IStreamCoder.Flags.FLAG2_MIXED_REFS, true);
				outCoder.setFlag(IStreamCoder.Flags.FLAG2_WPRED, true);
				outCoder.setFlag(IStreamCoder.Flags.FLAG2_8X8DCT, true);
				outCoder.setFlag(IStreamCoder.Flags.FLAG2_FASTPSKIP, true);
//				outCoder.setFlag(IStreamCoder.Flags.FLAG, true);*/
				// ローカルでの動作その２
				outCoder.setProperty("coder", "0");
				outCoder.setProperty("me_method", "hex");
				outCoder.setProperty("subq", "7");
				outCoder.setProperty("bf", "0");
				outCoder.setProperty("level", "13");
				outCoder.setProperty("me_range", "16");
				outCoder.setProperty("qdiff", "3");
				outCoder.setProperty("g", "150");
				outCoder.setProperty("qmin", "12");
				outCoder.setProperty("qmax", "30");
				outCoder.setProperty("refs", "3");
				outCoder.setProperty("qcomp", "0");
				outCoder.setProperty("maxrate", "600k");
				outCoder.setProperty("bufsize", "2000k");
				outCoder.setFlag(IStreamCoder.Flags.FLAG_LOOP_FILTER, true);
				// サーバー環境での動作
/*				outCoder.setProperty("coder", "0");
				outCoder.setProperty("bf", "0");
				outCoder.setProperty("level", "13");*/

				if (mOutputInfo.getVideoTimeBase() != null)
					outCoder.setTimeBase(mOutputInfo.getVideoTimeBase());
				else
					outCoder.setTimeBase(IRational.make(1, 1000)); // default to FLV

				outCoder.open();
				// if we get here w/o an exception, record the coder
				mOutVideoCoder = outCoder;
			}
		}
		retval = mOutContainer.writeHeader();
		if (retval < 0) {
			throw new RuntimeException("could not write header for output");
		}
	}

	private void openInputCoders(IPacket packet) {
		{
			IStreamCoder audioCoder = null;
			IStreamCoder videoCoder = null;
			if (mAudioStreamId == -1 || mVideoStreamId == -1) {
				int numStreams = mInContainer.getNumStreams();
				log.debug("found {} streams in {}", numStreams, mInputURL);

				for(int i  = 0; i < numStreams; i++) {
					IStream stream = mInContainer.getStream(i);
					if (stream != null) {
						log.debug("found stream #{} in {}", i, mInputURL);
						IStreamCoder coder = stream.getStreamCoder();
						if (coder != null) {
							log.debug("got stream coder {} (type: {}) in {}",
									new Object[]{coder, coder.getCodecType(), mInputURL});
							if (coder.getCodecType()==ICodec.Type.CODEC_TYPE_AUDIO // if audio
									&& mAudioStreamId == -1 // and we haven't already initialized
									&& packet.getStreamIndex() == i // and this packet is also audio
							) {
								log.debug("found audio stream: {} in {}", i, mInputURL);
								if (coder.getCodec() != null) {
									audioCoder = coder;
									mAudioStreamId = i;
								}
								else {
									log.error("could not find codec for audio stream: {}, {}", i, coder.getCodecID());
									throw new RuntimeException("Could not find codec for audio stream");
								}
							}
							if (coder.getCodecType()==ICodec.Type.CODEC_TYPE_VIDEO // if video
									&& mVideoStreamId == -1 // and we haven't already initialized
									&& packet.getStreamIndex() == i // and this packet is also video
							) {
								log.debug("found video stream: {} in {}", i, mInputURL);
								if (coder.getCodec() != null) {
									videoCoder = coder;
									mVideoStreamId = i;
								}
								else {
									log.error("could not find codec for video stream: {}, {}", i, coder.getCodecID());
									throw new RuntimeException("Could not find codec for video stream");
								}
							}
						}
					}
				}
			}
			if (mAudioStreamId != -1 && mInAudioCoder == null) {
				log.debug("opening input audio coder; codec id: {}; actual codec: {}; sample rate: {}; channels: {}",
						new Object[]{
						audioCoder.getCodecID(),
						audioCoder.getCodec().getID(),
						audioCoder.getSampleRate(),
						audioCoder.getChannels()
				});

				if (audioCoder.open() < 0) {
					throw new RuntimeException("could not open audio coder for stream: " + mAudioStreamId);
				}
				mInAudioCoder = audioCoder;
			}
			if (mVideoStreamId != -1 &&  mInVideoCoder == null) {
				log.debug("opening input video coder; codec id: {}; actual codec: {}; width: {}; height: {}",
						new Object[]{
						videoCoder.getCodecID(),
						videoCoder.getCodec().getID(),
						videoCoder.getWidth(),
						videoCoder.getHeight()
				});

				if (videoCoder.open() < 0) {
					throw new RuntimeException("could not open video coder for stream: " + mVideoStreamId);
				}
				mInVideoCoder = videoCoder;
			}
		}
	}

	private void openVideoResampler(IVideoPicture picture) {
		if (mVideoResampler == null && mOutVideoCoder != null) {
			if (picture.getWidth() <= 0 || picture.getHeight() <= 0) {
				throw new RuntimeException("frame has no data in it so cannot resample");
			}

			// We set up our resampler.
			if (mOutVideoCoder.getPixelType() != picture.getPixelType() ||
					mOutVideoCoder.getWidth() != picture.getWidth() ||
					mOutVideoCoder.getHeight() != picture.getHeight()) {
				mVideoResampler = IVideoResampler.make(
						mOutVideoCoder.getWidth(),
						mOutVideoCoder.getHeight(),
						mOutVideoCoder.getPixelType(),
						picture.getWidth(),
						picture.getHeight(),
						picture.getPixelType());
				if (mVideoResampler == null) {
					log.error("Could not create a video resampler; this object is only available in the GPL version of aaffmpeg");
					throw new RuntimeException("needed to resample video but couldn't allocate a resampler; you need the GPL version of AAFFMPEG installed?");
				}
				log.debug("Setup resample to convert \"{}x{} {} video\" to \"{}x{} {} video\" audio",
						new Object[]{
						mVideoResampler.getInputWidth(),
						mVideoResampler.getInputHeight(),
						mVideoResampler.getInputPixelFormat(),
						mVideoResampler.getOutputWidth(),
						mVideoResampler.getOutputHeight(),
						mVideoResampler.getOutputPixelFormat()
	            	});
			}
		}
	}

	private void openAudioResampler(IAudioSamples samples) {
		if (mAudioResampler == null && mOutAudioCoder != null) {
			if (mOutAudioCoder.getSampleRate() != samples.getSampleRate() ||
					mOutAudioCoder.getChannels() != samples.getChannels()) {
				mAudioResampler = IAudioResampler.make(
						mOutAudioCoder.getChannels(),
						samples.getChannels(),
						mOutAudioCoder.getSampleRate(),
						samples.getSampleRate());
				if (mAudioResampler == null) {
					throw new RuntimeException("needed to resample audio but couldn't allocate a resampler");
				}
				log.debug("Setup resample to convert \"{}khz {} channel audio\" to \"{}khz {} channel\" audio",
						new Object[]{
						mAudioResampler.getInputRate(),
						mAudioResampler.getInputChannels(),
						mAudioResampler.getOutputRate(),
						mAudioResampler.getOutputChannels()
				});
			}
			// and we write the output header
			log.debug("Converting \"{} {}khz {} channel\" input audio to \"{} {}khz {} channel\" output audio",
					new Object[]{
					mInAudioCoder.getCodecID().toString(),
					samples.getSampleRate(),
					samples.getChannels(),
					mOutAudioCoder.getCodecID().toString(),
					mOutAudioCoder.getSampleRate(),
					mOutAudioCoder.getChannels()
			});
		}
	}

	private void closeContainer() {
		try {
			mInputStream.removeStreamListener(mInputListener);
			if (mOutContainer != null)
				mOutContainer.writeTrailer();
			if (mOutAudioCoder != null)
				mOutAudioCoder.close();
			mOutAudioCoder = null;
			if (mInAudioCoder != null)
				mInAudioCoder.close();
			mInAudioCoder = null;
			if (mOutVideoCoder != null)
				mOutVideoCoder.close();
			mOutVideoCoder = null;
			if (mInVideoCoder != null)
				mInVideoCoder.close();
			mInVideoCoder = null;
			if (mOutContainer != null)
				mOutContainer.close();
			mOutContainer = null;
		}
		finally {
			synchronized(this) {
				mIsRunning = false;
				notifyAll();
			}
		}
	}

	private void decodeVideo(IPacket decodePacket) {
		int retval = -1;
		// Note that we don't specify the input width and height; the StreamCoder will fill that
		// in when it decodes
		IVideoPicture inPicture = IVideoPicture.make(mInVideoCoder.getPixelType(), mInVideoCoder.getWidth(), mInVideoCoder.getHeight());
		log.debug("made frame to decode into; type: {}; width: {}; height: {}",
				new Object[]{
				inPicture.getPixelType(),
				inPicture.getWidth(),
				inPicture.getHeight()
		});
		// resampled video
		IVideoPicture reSample = null;
		int offset = 0;
		while (offset < decodePacket.getSize()) {
			log.debug("ready to decode video; keyframe: {}", decodePacket.isKey());
			retval = mInVideoCoder.decodeVideo(inPicture, decodePacket, offset);
			log.debug("decode video completed; packet size: {}; offset: {}; bytes consumed: {}; frame complete: {}; width: {}; height: {}",
					new Object[]{
					decodePacket.getSize(),
					offset,
					retval,
					inPicture.isComplete(),
					inPicture.getWidth(),
					inPicture.getHeight()
			});
			if (retval <= 0) {
				log.info("Could not decode video: {}", retval);
				return;
			}
			offset += retval;

			IVideoPicture postDecode = inPicture;

			// ここでデコードが完了していたら、画像化
			if(postDecode.isComplete()) {
				JpegModel.makeFramePicture(postDecode, position);
				reSample = resampleVideo(postDecode);
			}
			else
			{
				reSample = postDecode;
			}
			if (reSample.isComplete())
			{
				encodeVideo(reSample);
			}
		}
	}

	/**
	 * あとでh.264にエンコードするときに必要。
	 * @param picture
	 */
	private void encodeVideo(IVideoPicture picture)
	{
		int retval;
		IPacket oPacket = IPacket.make();

		/**
		 * NOTE: At this point reSamples contains the actual unencoded raw samples.
		 * 
		 * The next step does an encoding, but you PROBABLY don't need to do that.
		 * Instead, you could copy the reSamples.getSamples().getData(...) bytes
		 * into your own structure and hand them off, but for now, we'll
		 * try re-encoding as FLV with PCM embedded.
		 */
		IVideoPicture preEncode= picture;
		int numBytesConsumed = 0;
		if (preEncode.isComplete()) {
			log.debug("ready to encode video");
			retval = mOutVideoCoder.encodeVideo(oPacket, preEncode, 0);
			if (retval <= 0) {
				// If we fail to encode, complain loudly but still keep going
				log.error("could not encode video picture; continuing anyway");
			} else {
				log.debug("encode video completed");
				numBytesConsumed += retval;
			}
			if (oPacket.isComplete()) {
				// ここまでこれたら、十分
				// このタイミングで必要なら、mp3の書き込みの続きを実施する。(音のない放送用:もしくは音が流れてこない放送対応)
	    		Mp3M3u8Model.updatePacketMp3(position);
	    		mOutContainer.writePacket(oPacket);
			}
		}
	}

	/**
	 * あとでh.264にエンコードするときに必要。
	 * @param picture
	 */
	private IVideoPicture resampleVideo(IVideoPicture picture)
	{
		IVideoPicture reSample;
	    
		openVideoResampler(picture);

		if (mVideoResampler != null) {
			log.debug("ready to resample video");

			IVideoPicture outPicture = IVideoPicture.make(mOutVideoCoder.getPixelType(), mOutVideoCoder.getWidth(), mOutVideoCoder.getHeight());

			IVideoPicture preResample= picture;
			int retval = -1;
			retval = mVideoResampler.resample(outPicture, preResample);
			if (retval < 0)
				throw new RuntimeException("could not resample video");
			log.debug("resampled input picture (type: {}; width: {}; height: {}) to output (type: {}; width: {}; height: {})",
					new Object[]{
					preResample.getPixelType(),
					preResample.getWidth(),
					preResample.getHeight(),
					outPicture.getPixelType(),
					outPicture.getWidth(),
					outPicture.getHeight()
			});
			IVideoPicture postResample= outPicture;
			reSample = postResample;
		} else {
			reSample = picture;
		}
		return reSample;
	}

	private void decodeAudio(IPacket decodePacket)
	{
		int retval = -1;
		IAudioSamples inSamples = IAudioSamples.make(1024, mInAudioCoder.getChannels());
		// resampled audio
		IAudioSamples reSamples = null;
		int offset = 0;
		while (offset < decodePacket.getSize()) {
			log.debug("ready to decode audio");
			retval = mInAudioCoder.decodeAudio(inSamples, decodePacket, offset);
			if (retval <= 0) {
				throw new RuntimeException("could not decode audio");
			}
			log.debug("decode audio completed");
			offset += retval;

			IAudioSamples postDecode = inSamples;

			if (postDecode.isComplete())
			{
				reSamples = resampleAudio(postDecode);
			} else
			{
				reSamples = postDecode;
			}

			if (reSamples.isComplete())
			{
				encodeAudio(reSamples);
			}
		}
	}

	private void encodeAudio(IAudioSamples samples)
	{
		int retval;
	    IPacket oPacket = IPacket.make();

	    /**
	     * NOTE: At this point reSamples contains the actual unencoded raw samples.
	     * 
	     * The next step does an encoding, but you PROBABLY don't need to do that.
	     * Instead, you could copy the reSamples.getSamples().getData(...) bytes
	     * into your own structure and hand them off, but for now, we'll
	     * try re-encoding as FLV with PCM embedded.
	     */
	    IAudioSamples preEncode= samples;

	    int numSamplesConsumed = 0;
	    while (numSamplesConsumed < preEncode.getNumSamples()) {
	    	log.debug("ready to encode audio");

	    	retval = mOutAudioCoder.encodeAudio(oPacket, preEncode,
	    			numSamplesConsumed);
	    	if (retval <= 0) {
	    		// If we fail to encode, complain loudly but still keep going
	    		log.error("could not encode audio samples; continuing anyway");
	    		// and break the loop since these samples are now suspect
	    		break;
	    	}
	    	log.debug("encode audio completed");

	    	numSamplesConsumed += retval;

	    	if (oPacket.isComplete()) {
	    		// TODO この時点でmp3のパケットになっているので、2秒分ずつ、m3u8化していく。
	    		ByteBuffer b = oPacket.getByteBuffer();
	    		byte[] bytes = new byte[b.limit()];
	    		b.get(bytes);
	    		Mp3M3u8Model.makePacketMp3(bytes, position);
	    		mOutContainer.writePacket(oPacket);
	    	}
	    }
	}

	private IAudioSamples resampleAudio(IAudioSamples samples)
	{
		IAudioSamples reSamples;
		openAudioResampler(samples);

		IAudioSamples outSamples = IAudioSamples.make(1024, mOutAudioCoder.getChannels());

		if (mAudioResampler != null && samples.getNumSamples() > 0) {
			log.debug("ready to resample audio");
			IAudioSamples preResample= samples;
			int retval = -1;
			retval = mAudioResampler.resample(outSamples, preResample,
					preResample.getNumSamples());
			if (retval < 0)
				throw new RuntimeException("could not resample audio");
			log.debug("resampled {} input samples ({}khz {} channels) to {} output samples ({}khz {} channels)",
					new Object[]{
					preResample.getNumSamples(),
					preResample.getSampleRate(),
					preResample.getChannels(),
					outSamples.getNumSamples(),
					outSamples.getSampleRate(),
					outSamples.getChannels()
			});

			IAudioSamples postResample= outSamples;
			reSamples = postResample;
		} else {
			reSamples = samples;
		}
		return reSamples;
	}
}
