package com.ttProject.xuggle.red5;

import java.util.HashMap;
import java.util.Map;

import com.xuggle.xuggler.ISimpleMediaFile;
import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.io.IURLProtocolHandlerFactory;
import com.xuggle.xuggler.io.URLProtocolManager;

public class Red5HandlerFactory implements IURLProtocolHandlerFactory{
	private Map<String, IRTMPEventIOHandler> mStreams;
	private Map<String, ISimpleMediaFile> mStreamsInfo;

	private static Red5HandlerFactory mSingleton = new Red5HandlerFactory();
	public static final String DEFAULT_PROTOCOL="redfive";

	/**
	 * Get the default factory used by AAFFMPEG.IO for red-5 streams to
	 * {@link #DEFAULT_PROTOCOL} (i.e. {@value #DEFAULT_PROTOCOL}).
	 * @return The factory
	 */
	static public Red5HandlerFactory getFactory() {
		return getFactory(DEFAULT_PROTOCOL);
	}

	/**
	 * Register a factory that AAFFMPEG.IO will use for the given protocol.
	 * 
	 * NOTE: Protocol can only contain alpha characters.
	 * @param protocolPrefix The protocol (e.g. "redfive").
	 * @return The factory
	 */
	static public Red5HandlerFactory getFactory(String protocolPrefix) {
		if (mSingleton==null) {
			throw new NullPointerException("unexpectedly, there is no factory");
		}

		URLProtocolManager manager = URLProtocolManager.getManager();
		manager.registerFactory(protocolPrefix, mSingleton);

		return mSingleton;
	}

	/**
	 * The Constructor is package-level only so that test functions
	 * can use this without using the Singleton.
	 */
	Red5HandlerFactory() {
		mStreams = new HashMap<String, IRTMPEventIOHandler>();
		mStreamsInfo = new HashMap<String, ISimpleMediaFile>();
	}

	/**
	 * Called by AAFFMPEG.IO to get a a handler for a given URL.  The
	 * handler must have been registered via
	 * {@link #registerStream(IRTMPEventIOHandler, ISimpleMediaFile)}
	 * 
	 * WARNING: It really only makes sense to have one active ProtocolHandler
	 * working on a AVStreamingQueue at a time; it's up to the caller to
	 * ensure this happens; otherwise you may get deadlocks as all the
	 * protocol handlers compete for events.
	 * 
	 * @param protocol The protocol (e.g. "redfive")
	 * @param url The url being opened, including the protocol (e.g. "redfive:stream01")
	 * @param flags The flags that FFMPEG is opening the file with.
	 * 
	 */
	public synchronized IURLProtocolHandler getHandler(String protocol,
			String url, int flags) {
		IURLProtocolHandler result = null;

		// Note: We need to remove any protocol markers from the url
		String streamName = URLProtocolManager.getResourceFromURL(url); 

		IRTMPEventIOHandler handler = mStreams.get(streamName);
		ISimpleMediaFile info = mStreamsInfo.get(streamName);
		if (handler!= null) {
			result = new Red5Handler(handler, info, url, flags);
		}
		return result;
	}

	/**
	 * Register a stream name with this factory.  Any handlers returned by
	 * this factory for this streamName will use the passed buffer.
	 * 
	 * Note that streamInfo is only used when reading from a registered stream.
	 * When writing IRTMPEvent, we will use whatever MetaData AAFFMPEG tells
	 * us about the stream.
	 * 
	 * @param handler The handler for that stream name
	 * @param streamInfo A {@link ISimpleMediaFile} containing meta data about what kind
	 *   of stream the {@link IRTMPEventIOHandler} is handling.  This is used by the
	 *   handler to construct header and meta-data information.  Can be null in which
	 *   case we'll assume both audio and video in file.  {@link ISimpleMediaFile#getURL()}
	 *   must return a non null value.
	 * @return The IRTMPEventIOHandler previously registered for this
	 *   streamName, or null if none.
	 */
	public synchronized IRTMPEventIOHandler registerStream(
			IRTMPEventIOHandler handler, ISimpleMediaFile streamInfo) {
		if (streamInfo == null) {
			throw new IllegalArgumentException("need streaminfo");
		}
		String streamURL = streamInfo.getURL();
		if (streamURL == null) {
			throw new IllegalArgumentException("need URL set");
		}
		String streamName = URLProtocolManager.getResourceFromURL(streamURL);

		mStreamsInfo.put(streamName, streamInfo);
	    return mStreams.put(streamName, handler);
	}

	/**
	 * Stop supporting a given streamName.
	 * @param streamURL The stream url to stop supporting.  Current files open will continue to
	 *  to be used, but future opens will get a file not found error in ffmpeg.
	 * @return The AVBufferStream previously registered for this streamName, or null if none.
	 */
	public synchronized IRTMPEventIOHandler deleteStream(
			String streamURL) {
		String streamName = URLProtocolManager.getResourceFromURL(streamURL); 
		mStreamsInfo.remove(streamName);
		return mStreams.remove(streamName);
	}
}
