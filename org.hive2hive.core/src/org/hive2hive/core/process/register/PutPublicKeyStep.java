package org.hive2hive.core.process.register;

import net.tomp2p.futures.FutureDHT;

import org.hive2hive.core.H2HConstants;
import org.hive2hive.core.model.UserProfile;
import org.hive2hive.core.model.UserPublicKey;
import org.hive2hive.core.network.messages.direct.response.ResponseMessage;
import org.hive2hive.core.process.PutProcessStep;

/**
 * Puts the user's public key to the network (which is used for encryption of messages and other
 * communication)
 * 
 * @author Nico
 * 
 */
public class PutPublicKeyStep extends PutProcessStep {

	private UserProfile userProfile;

	protected PutPublicKeyStep(UserProfile userProfile) {
		super(null);
		this.userProfile = userProfile;
	}

	@Override
	public void start() {
		put(userProfile.getUserId(), H2HConstants.USER_PUBLIC_KEY, new UserPublicKey(userProfile
				.getEncryptionKeys().getPublic()));
	}

	@Override
	public void rollBack() {
		super.rollBackPut(userProfile.getUserId(), H2HConstants.USER_PUBLIC_KEY);
	}

	@Override
	protected void handleMessageReply(ResponseMessage asyncReturnMessage) {
		// not used
	}

	@Override
	protected void handlePutResult(FutureDHT future) {
		if (future.isSuccess()) {
			// TODO: next step?
			getProcess().nextStep(null);
		} else {
			rollBack();
		}
	}

	@Override
	protected void handleGetResult(FutureDHT future) {
		// not used
	}

	@Override
	protected void handleRemovalResult(FutureDHT future) {
		// not used
	}

}