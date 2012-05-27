package com.ttProject.xuggle.red5;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.Output;
import org.red5.io.object.Serializer;
import org.red5.io.utils.HexDump;
import org.red5.io.utils.IOUtils;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.stream.IStreamData;
import org.slf4j.Logger;

import com.ttProject.xuggle.Transcoder;
import com.ttProject.xuggle.red5.Red5Message.Type;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.ITimeValue;
import com.xuggle.xuggler.SimpleMediaFile;
import com.xuggle.xuggler.io.IURLProtocolHandler;

public class Red5Handler implements IURLProtocolHandler{
	private static final int FLV_FILE_HEADER_SIZE = 9 + 4;
	private static final int FLV_TAG_HEADER_SIZE = 11 + 4;
	final private Logger log = Red5LoggerFactory.getLogger(this.getClass());
	
	IRTMPEventIOHandler mHandler;
	
	IoBuffer mCurrentInputBuffer = null;
	IoBuffer mCurrentOutputBuffer = null;
	
	String mUrl;
	int mOpenFlags;
	
	private long mReadPosition = 0;
	
	private boolean mReadHeader = false;
	private boolean mReadEndOfStream = false;
	private boolean mReadMetaData = false;

	private final ISimpleMediaFile mStreamInfo;
	
	public Red5Handler(IRTMPEventIOHandler handler,
			ISimpleMediaFile metaInfo,
			String url,
			int flags) {
		if(metaInfo == null) {
			metaInfo = new SimpleMediaFile();
		}
		mStreamInfo = metaInfo;
		mHandler = handler;
	}
	private int unsafe_close() throws InterruptedException {
		// Odd, but FFMPEG told us to close.  For now, we just
		// ignore.  Long term, we should probably signal the
		// BufferStream that we're no longer actively handling it.
		if (mCurrentInputBuffer != null) {
			mCurrentInputBuffer = null;
		}
		if (mCurrentOutputBuffer != null) {
			mCurrentOutputBuffer = null;
		}
		if (mOpenFlags == IURLProtocolHandler.URL_RDWR
				|| mOpenFlags == IURLProtocolHandler.URL_WRONLY_MODE) {
			// As a convention, we send a IMediaDataWrapper object wrapping NULL for end of streams
			mHandler.write(new Red5Message(Type.END_STREAM, null));
		}
		return 0;
	}

	private int unsafe_open(String url, int flags)
	{
		// For an open, we assume the ProtocolManager has done it's job
		// correctly and we're working on the right input and output
		// streams.
		mUrl = url;
		mOpenFlags = flags;

		if (mCurrentInputBuffer != null) {
			mCurrentInputBuffer = null;
		}
		if (mCurrentOutputBuffer != null) {
			mCurrentOutputBuffer = null;
		}
		mReadEndOfStream = false;
		mReadHeader = false;
		mReadMetaData = false;

		return 0;
	}

	private int unsafe_read(byte[] buf, int size) throws InterruptedException {
		int bytesToRead = 0;

		if (mReadEndOfStream) {
			// we've read the end of stream already; we shouldn't be reading any more.
			return -1;
		}

		// if we don't have a buffer, create one.
		if (mCurrentInputBuffer == null) {
			// start with the requested side.
			mCurrentInputBuffer = IoBuffer.allocate(size); 

			// and let it auto grow.
			mCurrentInputBuffer.setAutoExpand(true);
			// and set the limit to zero.
			mCurrentInputBuffer.flip();
		}
		bytesToRead = mCurrentInputBuffer.remaining();

		while (bytesToRead < size && bytesToRead == 0 && !mReadEndOfStream) {
			// keep getting new messages until we have enough
			// to fill the requested buffer.
			// now the magic begins.
			IRTMPEvent event = null;
			Red5Message msg = null;

			try {
				msg = mHandler.read();
				// TODO このタイミングでrtmpのデータを呼び出しします。
				Red5Message.Type type = msg.getType();
				event = msg.getData();
				Transcoder.setPosition(event.getTimestamp());
				switch (type) {
				case HEADER:
					// We need to create and send a header.
					if (mReadHeader) {
						log.warn("Already sent a header for this stream; ignoring");
					}
					else {
						appendFLVHeader(mCurrentInputBuffer);
						mReadHeader = true;
						mReadEndOfStream = false;
					}
					break;
				case END_STREAM:
					// end of stream; send back -1;
					mReadHeader = false;
					mReadEndOfStream = true;
					break;
				case INTERFRAME:
				case DISPOSABLE_INTERFRAME:
				case KEY_FRAME:
				case AUDIO:
				case OTHER:
					// Valid stream data; parse and send in right
					// format to the caller
					if (!mReadHeader) {
						log.warn("Starting to send messages for a stream, but "
								+ "we have not yet sent a header.  Faking "
								+ "it until we make it: {}", msg);
						// we need to jam a header in.
						appendFLVHeader(mCurrentInputBuffer);
						mReadHeader = true;
					}
					if (!mReadMetaData ) {
						if (event.getDataType() == 0x12) {
							mReadMetaData = true;
						}
						else {
							// fake the meta data
							log.debug("did not get meta data as first packet; so faking some meta data: {}", msg);
							appendMetaData(0, mCurrentInputBuffer);
							mReadMetaData=true;
						}
					}
					appendRTMPEvent(event, mCurrentInputBuffer);
					break;
				}
			}
			finally {
				if (event != null) {
					event.release();
					event = null;
				}
			}
			bytesToRead = mCurrentInputBuffer.remaining();
			log.trace("Bytes to read: {}; size: {}", bytesToRead, size);
		}

		// and copy into the requested buffer
		int bytesToCopy = Math.min(size, bytesToRead);

		if (bytesToCopy > 0) {
			// TODO bufの中にflvデータをコピーしてそれを投げてるだけみたいです。
			// ffmpegのパケット応答が勝手に動画と音声にわけてる？
			mCurrentInputBuffer.get(buf, 0, bytesToCopy);
			// remove the read bytes
			mCurrentInputBuffer.compact();
			// and set the limit to the bytes remaining
			mCurrentInputBuffer.flip();
		}
		mReadPosition += bytesToCopy;
		return bytesToCopy;
	}

	private long unsafe_seek(long offset, int whence) {
		long newPos = -1;

		switch (whence) {
		case SEEK_END:
		case SEEK_CUR:
		case SEEK_SET:
		case SEEK_SIZE:
		default:
			// Unsupported
			newPos = -1;
			break;
		}

		return newPos;
	}
	private boolean unsafe_isStreamed(String url, int flags) {
		return true;
	}

	public String toString() {
		return this.getClass().getName() + ":" + mUrl;
	}

	private void appendFLVTag(byte dataType, int timestamp,
			IoBuffer data, IoBuffer in) {
		int oldPos = in.position();
		int bufEnd = in.limit();
		// put us at the end of the buffer
		in.position(bufEnd);

		data.rewind();
		int msgBodySize = data.remaining();

		in.put(dataType);

		// Body Size (this method writes the bottom 3 bytes as
		// a 24-byte integer).
		IOUtils.writeMediumInt(in, (msgBodySize));

		// Timestamp; FLV Timestamps are annoying.
		// First 3 bytes is the UPPER 
		byte[] timestampBytes = new byte[4];
		timestampBytes[0] = (byte) ((timestamp >>> 16) & 0xFF);
		timestampBytes[1] = (byte) ((timestamp >>> 8) & 0xFF);
		timestampBytes[2] = (byte) (timestamp & 0xFF);
		timestampBytes[3] = (byte) ((timestamp >>> 24) & 0xFF);
		in.put(timestampBytes);

		// Reserved 3 bytes set to zero (StreamId)
		IOUtils.writeMediumInt(in, 0);

		// Now, write out out the actual data
		in.put(data);

		// Officially we're done with the tag, and now are in the
		// next message in the FLV stream.
		// Which happens to be the size of the previous tag.
		in.putInt(msgBodySize + FLV_TAG_HEADER_SIZE - 4);

		// now reset us back to the old position
		in.position(oldPos);
	}

	@SuppressWarnings("rawtypes")
	private void appendRTMPEvent(IRTMPEvent event, IoBuffer in) {
		IoBuffer data = null;

		if (event instanceof IStreamData) {
			data = ((IStreamData) event).getData();
		}
		else if (event instanceof Unknown) {
			data = ((Unknown) event).getData();
		}
		if (data != null) {
			appendFLVTag(event.getDataType(), event.getTimestamp(), data, in);
		}
		else {
			throw new RuntimeException("No data in tag.");
		}
	}

	private void appendFLVHeader(IoBuffer in)
	{
		int oldPos = in.position();
		// set us to the end of the buffer
		in.position(in.limit());

		// Add the header items.

		// 'F'
		in.put((byte) 0x46);
		// 'L'
		in.put((byte) 0x4C);
		// 'V'
		in.put((byte) 0x56);

		// Write version: always 1
		in.put((byte) 0x01);

		// Both video and audio, which is 0x04 & 0x01 or 0x05
		// IMPORTANT NOTE:
		// There is a "feature" in FFMPEG that wreaks havoc with this setting.  If 
		// we don't say that every stream has both audio and video, FFMPEG will assume a new
		// stream may show up at any point in time and add a video stream.  As a result when
		// you try to get information about the file, it will keep reading ahead until it finds
		// the first audio and the first video packet.
		// However if you NEVER add a video or audio packet, then we will hang forever.

		// AAFFMPEG works around this by clearing this flag for FLV files; but then we MUST
		// ensure we never ever add data audio or video data if we said it wouldn't be there.

		byte audioVideoFlag = 0;
		if (mStreamInfo.hasAudio()) {
			audioVideoFlag |= 0x04;
		}
		if (mStreamInfo.hasVideo()) {
			audioVideoFlag |= 0x01;
		}
		in.put(audioVideoFlag);

		// Total size of header, not including the 4 bytes for the "last tag"
		in.putInt(FLV_FILE_HEADER_SIZE - 4);

		// And the size of the last tag, which for the header is always zero.
		// Always zero
		in.putInt(0);

		// and put us back where we were before
		in.position(oldPos);
	}

	private void addMetaData(Map<Object, Object> params, String key, Number value) {
		log.debug("add metaData[{}]={}", key, value);
		params.put(key, value);
	}
	private void addMetaData(Map<Object, Object> params, String key, Boolean value) {
		log.debug("add metaData[{}]={}", key, value);
		params.put(key, value);
	}  
	private void appendMetaData(int timestamp, IoBuffer in) {
		log.trace("Writing meta data for FFMPEG");
		Map<Object, Object> params = new HashMap<Object, Object>(4);
		if (mStreamInfo.hasAudio()) {
			if (mStreamInfo.isAudioChannelsKnown()) {
				boolean value = mStreamInfo.getAudioChannels()!=1;
				addMetaData(params, "stereo", Boolean.valueOf(value));
			}
			if (mStreamInfo.isAudioSampleRateKnown()) {
				int value = mStreamInfo.getAudioSampleRate();
				addMetaData(params, "audiosamplerate", new Double(value));
			}
			{
				int value = 16;
				addMetaData(params, "audiosamplesize", new Double(value));
			}
			ICodec.ID audCodec = mStreamInfo.getAudioCodec();
			if (audCodec != null && audCodec != ICodec.ID.CODEC_ID_NONE) {
				int flvCodecID = 0;
				switch(audCodec) {
				case CODEC_ID_PCM_S16BE:
					flvCodecID = 0;
					break;
				case CODEC_ID_PCM_S16LE:
					flvCodecID = 3;
					break;
				case CODEC_ID_ADPCM_SWF:
					flvCodecID = 1;
					break;
				case CODEC_ID_MP3:
					if (mStreamInfo.isAudioSampleRateKnown() && mStreamInfo.getAudioSampleRate()==8000) {
						flvCodecID = 14;
					}
					else {
						flvCodecID = 2;
					}
					break;
				case CODEC_ID_NELLYMOSER:
					if (mStreamInfo.isAudioSampleRateKnown() && mStreamInfo.getAudioSampleRate()==8000) {
						flvCodecID = 5;
					}
					else {
						flvCodecID = 6;
					}
					break;
				case CODEC_ID_AAC:
					flvCodecID = 10;
					break;
				case CODEC_ID_SPEEX:
					flvCodecID = 11;
					break;
				}
				{
					int value = flvCodecID;
					addMetaData(params, "audiocodecid", new Double(value));
				}
			}
		}
		if (mStreamInfo.hasVideo()) {
			if (mStreamInfo.isVideoWidthKnown()) {
				double value = mStreamInfo.getVideoWidth();
				addMetaData(params, "width", new Double(value));
			}
			if (mStreamInfo.isVideoHeightKnown()) {
				double value = mStreamInfo.getVideoHeight();
				addMetaData(params, "height", new Double(value));
			}
			if (mStreamInfo.isVideoBitRateKnown()) {
				double value = mStreamInfo.getVideoBitRate() / 1024.0; // in kbits per sec
				addMetaData(params, "videodatarate", new Double(value));
			}
			IRational frameRate = mStreamInfo.getVideoFrameRate();
			if (frameRate != null) {
				double value = frameRate.getDouble();
				addMetaData(params, "framerate", new Double(value));
			}
			ICodec.ID vidCodec = mStreamInfo.getVideoCodec();
			if (vidCodec != null &&
					vidCodec != ICodec.ID.CODEC_ID_NONE) {
				int flvCodecID = 2;
				switch(vidCodec) {
				case CODEC_ID_FLV1:
					flvCodecID = 2;
					break;
				case CODEC_ID_FLASHSV:
					flvCodecID = 3;
					break;
				case CODEC_ID_VP6F:
					flvCodecID = 4;
					break;
				case CODEC_ID_VP6A:
					flvCodecID = 5;
					break;
				case CODEC_ID_H264:
					flvCodecID = 7;
					break;
				}
				{
					int value = flvCodecID;
					addMetaData(params, "videocodecid", new Double(value));
				}
			}
		}
		ITimeValue duration = mStreamInfo.getDuration();
		if (duration != null) {
			double value = duration.get(ITimeValue.Unit.MILLISECONDS) / 1000.0;
			addMetaData(params, "duration", new Double(value));
		}

		// hey by default we're NON seekable streams; deal with it.
		addMetaData(params, "canSeekToEnd", Boolean.FALSE);

		IoBuffer amfData = IoBuffer.allocate(1024);
		amfData.setAutoExpand(true);
		Output amfOutput = new Output(amfData);
		amfOutput.writeString("onMetaData");

		amfOutput.writeMap(params, new Serializer());

		amfData.flip();
		log.trace("Writing AMF object to stream: {} bytes; first byte: {}",
				amfData.remaining(),
				amfData.get(0));
		appendFLVTag((byte)0x12, 0, amfData, in);
	}

	/*
	 * These following methods all wrap the unsafe methods in
	 * try {} catch {} blocks to ensure we don't pass an exception
	 * back to the native C++ function that calls these.
	 * 
	 * (non-Javadoc)
	 * @see com.xuggle.videojuggler.ffmpegio.IURLProtocolHandler#close()
	 */
	public int close() {
		int retval = -1;
		try {
			retval = unsafe_close();
		}
		catch (Exception ex) {
			log.error("got uncaught exception: {}", ex);
		}
		log.trace("close({}); {}", mUrl, retval);
		return retval;
	}

	public int open(String url, int flags) {
		int retval = -1;
		try {
			retval = unsafe_open(url, flags);
		}
		catch (Exception ex) {
			log.error("got uncaught exception: {}", ex);
		}
		log.trace("open({}, {}); {}", new Object[] { url, flags, retval });
		return retval;
	}

	public int read(byte[] buf, int size) {
		int retval = -1;
		try {
			retval = unsafe_read(buf, size);
		}
		catch (Exception ex) {
			log.error("got uncaught exception: {}", ex);
		}
		log.trace("read({}, {}); {}", new Object[] { mUrl, size, retval });
		return retval;
	}

	public long seek(long offset, int whence) {
		long retval = -1;
		try {
			retval = unsafe_seek(offset, whence);
		}
		catch (Exception ex) {
			log.error("got uncaught exception: {}", ex);
		}
		return retval;
	}

	public int write(byte[] buf, int size) {
//		System.out.println(HexDump.toHexString(buf));
		// 書き込み済みバイト数を監視する？
		// ここでファイル別に書き込みを実行する必要があると思う。
		// 書き込みはそもそも実行しない。
		return size;
	}

	public boolean isStreamed(String url, int flags) {
		boolean retval = false;
		try {
			retval = unsafe_isStreamed(url, flags);
		}
		catch (Exception ex) {
			log.error("got uncaught exception: {}", ex);
		}
		log.trace("isStreamed({}, {}); {}", new Object[] { mUrl, flags, retval });

		return retval;
	}
}
