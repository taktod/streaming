package com.ttProject.xuggle.red5;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

import com.ttProject.xuggle.TimeValue;
import com.ttProject.xuggle.red5.Red5Message.Type;

/**
 * red5のストリーミングデータを扱うためのqueue動作
 * @author taktod
 */
public class Red5StreamingQueue extends LinkedBlockingQueue<Red5Message> implements IRTMPEventIOHandler {
	/** ログ */
	final private Logger log = Red5LoggerFactory.getLogger(this.getClass());
	/** 動作用のシリアルID */
	private static final long serialVersionUID = 1L;
	private TimeValue mReadTimeout = null;
	private TimeValue mWriteTimeout = null;
	private Queue<Red5Message> mCacheQueue = new LinkedList<Red5Message>();

	public Red5StreamingQueue() {
		log.trace("<init>");
	}

	public Red5Message read() throws InterruptedException {
		Red5Message result = null;

		if (mCacheQueue.size() > 0) {
			result = mCacheQueue.poll();
		}
		else {
			// Always drain currently available to the cache; this method doesn't
			// block, so we may need to block later.
			this.drainTo(mCacheQueue);

			// See if the drain added anything to the cache 
			if (mCacheQueue.size() > 0) {
				result = mCacheQueue.poll();
			}
			else {
				if (mReadTimeout == null) {
					result = super.take();
				}
				else {
					result = super.poll(mReadTimeout.get(TimeUnit.MICROSECONDS), TimeUnit.MICROSECONDS);
            	}
			}
		}
		if (result == null) {
			result = new Red5Message(Type.END_STREAM, null);
		}
		return result;
	}

	public void write(Red5Message msg) throws InterruptedException {
		//    log.debug("PRE  put: {}", msg);
		if (mWriteTimeout==null) {
			super.put(msg);
		}
		else {
			super.offer(msg, mWriteTimeout.get(TimeUnit.MICROSECONDS), TimeUnit.MICROSECONDS);
		}
	}

	/**
	 * Set the amount of time we will wait on a queue before giving up on reading.  If
	 * null, we will wait forever.
	 * @param readTimeout the readTimeout to set.  null means wait forever.
	 */
	public void setReadTimeout(TimeValue readTimeout) {
		mReadTimeout = readTimeout;
	}

	/**
	 * The amount of time we'll wait on a queue before giving up on reading.  If null,
	 * we will wait forever.
	 * 
	 * @return the readTimeout
	 */
	public TimeValue getReadTimeout() {
		return mReadTimeout;
	}

	/**
	 * Set the amount of time we'll wait when adding to the queue.  Null means we'll wait forever.
	 * @param writeTimeout the writeTimeout to set
	 */
	public void setWriteTimeout(TimeValue writeTimeout) {
		mWriteTimeout = writeTimeout;
	}

	/**
	 * Get the amount of time we'll wait when adding to the the queue.  Null means we'll wait forever. 
	 * @return the writeTimeout
	 */
	public TimeValue getWriteTimeout() {
		return mWriteTimeout;
	}
}
