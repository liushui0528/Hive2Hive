package org.hive2hive.core.network.data.futures;

import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureGet;
import net.tomp2p.futures.FutureRemove;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

import org.apache.log4j.Logger;
import org.hive2hive.core.H2HConstants;
import org.hive2hive.core.log.H2HLoggerFactory;
import org.hive2hive.core.network.data.DataManager;
import org.hive2hive.core.network.data.listener.IRemoveUserProfileTaskListener;
import org.hive2hive.core.network.usermessages.UserProfileTask;

/**
 * A future listener for a remove of a {@link UserProfileTask} object. After the operation completed the
 * listener verifies with a get if all data has been deleted. If not, the listener retries the remove (see
 * {@link H2HConstants#REMOVE_RETRIES}). In both cases the given {@link IRemoveUserProfileTaskListener}
 * listener gets notified. </br></br>
 * For further details see
 * {@link DataManager#removeUserProfileTask(String, Number160, IRemoveUserProfileTaskListener)}
 * 
 * @author Seppi
 */
public class FutureRemoveUserProfileTaskListener extends BaseFutureAdapter<FutureRemove> {

	private final static Logger logger = H2HLoggerFactory
			.getLogger(FutureRemoveUserProfileTaskListener.class);

	// used to count remove retries
	private int removeTries = 0;

	protected final String locationKey;
	protected final Number160 contentKey;
	protected final IRemoveUserProfileTaskListener listener;
	protected final DataManager dataManager;

	public FutureRemoveUserProfileTaskListener(String locationKey, Number160 contentKey,
			IRemoveUserProfileTaskListener listener, DataManager dataManager) {
		this.locationKey = locationKey;
		this.contentKey = contentKey;
		this.listener = listener;
		this.dataManager = dataManager;
	}

	@Override
	public void operationComplete(FutureRemove future) throws Exception {
		logger.debug(String.format(
				"Start verification of user profile task remove. locationKey = '%s' contentKey = '%s'",
				locationKey, contentKey));
		verifyWithAGet();
	}

	private void retryRemove() {
		// check if the threshold has been reached
		if (removeTries++ < H2HConstants.REMOVE_RETRIES) {
			logger.warn(String
					.format("Remove verification failed. Data is not null. Try #%s. location key = '%s' content key = '%s'",
							removeTries, locationKey, contentKey));
			dataManager.removeUserProfileTask(locationKey, contentKey).addListener(this);
		} else {
			logger.error(String
					.format("Remove verification failed. Data is not null after %s tries. location key = '%s' content key = '%s'",
							removeTries - 1, locationKey, contentKey));
			if (listener != null)
				listener.onRemoveUserProfileTaskFailure();
		}
	}

	private void verifyWithAGet() {
		// get data to verify if everything went correct
		FutureGet getFuture = dataManager.getUserProfileTask(locationKey, contentKey);
		getFuture.addListener(new BaseFutureAdapter<FutureGet>() {
			@Override
			public void operationComplete(FutureGet future) throws Exception {
				// analyze returned data and check if all data objects are empty or null
				for (PeerAddress peeradress : future.getRawData().keySet()) {
					for (Data data : future.getRawData().get(peeradress).values()) {
						if (data != null && data.object() != null) {
							retryRemove();
							return;
						}
					}
				}
				logger.debug(String
						.format("Verification for remove user profile task completed. location key = '%s' content key = '%s'",
								locationKey, contentKey));
				if (listener != null)
					listener.onRemoveUserProfileTaskSuccess();
			}
		});
	}
}
