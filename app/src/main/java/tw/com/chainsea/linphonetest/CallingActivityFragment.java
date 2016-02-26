package tw.com.chainsea.linphonetest;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneEvent;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneInfoMessage;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PublishState;
import org.linphone.core.Reason;
import org.linphone.core.SubscriptionState;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;


/**
 * A placeholder fragment containing a simple view.
 */
public class CallingActivityFragment extends Fragment implements LinphoneCoreListener.LinphoneListener, View.OnClickListener {
    final String TAG = "pengtao" + getClass().getSimpleName();
    View mView = null;

    LinphoneCore mLc = null;

    public CallingActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLc = LinphoneManager.getLc();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_calling, container, false);
        mView.findViewById(R.id.call).setOnClickListener(this);
        mView.findViewById(R.id.hangup).setOnClickListener(this);
        mView.findViewById(R.id.mic).setOnClickListener(this);
        mView.findViewById(R.id.speaker).setOnClickListener(this);
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLc.addListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            registerUserAuth(Utility.getUsername(getActivity()), Utility.getPassword(getActivity()), Utility.getHost(getActivity()));
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void callState(LinphoneCore linphoneCore, final LinphoneCall linphoneCall, LinphoneCall.State state, String s) {
        Log.e(TAG, "callState = " + state.toString());

        if ( state == LinphoneCall.State.IncomingReceived ) {
            mView.findViewById(R.id.receive_call).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        mLc.acceptCall(linphoneCall);
                    } catch (LinphoneCoreException e) {
                        e.printStackTrace();
                    }
                    mView.findViewById(R.id.receive_call).setVisibility(View.GONE);
                }
            });
            mView.findViewById(R.id.decline).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLc.declineCall(linphoneCall, Reason.None);
                    mView.findViewById(R.id.receive_call).setVisibility(View.GONE);
                }
            });
        }
    }

    public void registerUserAuth(String name, String password, String host) throws LinphoneCoreException {
        Log.e(TAG, "registerUserAuth name = " + name);
        Log.e(TAG, "registerUserAuth pw = " + password);
        Log.e(TAG, "registerUserAuth host = " + host);

        String identity = "sip:" + name + "@" + host;
        String proxy = "sip:" + host;

        LinphoneAddress proxyAddr = LinphoneCoreFactory.instance().createLinphoneAddress(proxy);
        LinphoneAddress identityAddr = LinphoneCoreFactory.instance().createLinphoneAddress(identity);

        LinphoneAuthInfo authInfo = LinphoneCoreFactory.instance().createAuthInfo(name, null, password, null, null, host);

        LinphoneProxyConfig prxCfg = mLc.createProxyConfig(identityAddr.asString(), proxyAddr.asStringUriOnly(), proxyAddr.asStringUriOnly(), true);

        prxCfg.enableAvpf(false);
        prxCfg.setAvpfRRInterval(0);
        prxCfg.enableQualityReporting(false);
        prxCfg.setQualityReportingCollector(null);
        prxCfg.setQualityReportingInterval(0);

        prxCfg.enableRegister(true);

        mLc.addProxyConfig(prxCfg);
        mLc.addAuthInfo(authInfo);

        mLc.setDefaultProxyConfig(prxCfg);
    }

    public void setCallingTo(String callto, String host) {
        LinphoneAddress lAddress;
        try {
            lAddress = mLc.interpretUrl(callto + "@" + host);
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
            return;
        }
        lAddress.setDisplayName("Chris");

        LinphoneCallParams params = mLc.createDefaultCallParameters();
        params.setVideoEnabled(false);
        try {
            mLc.inviteAddressWithParams(lAddress, params);
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.call:
                EditText editText = (EditText)mView.findViewById(R.id.input);
                setCallingTo(editText.getText().toString().trim(), Utility.getHost(getActivity()));
                Log.e(TAG, editText.getText().toString().trim());

                break;
            case R.id.mic:
                toggleMicro();
                break;
            case R.id.speaker:
                toggleSpeaker();
                break;
            case R.id.hangup:
                hangUp();
                break;

        }
    }

    public void hangUp() {
        LinphoneCall currentCall = mLc.getCurrentCall();

        if (currentCall != null) {
            mLc.terminateCall(currentCall);
        } else if (mLc.isInConference()) {
            mLc.terminateConference();
        } else {
            mLc.terminateAllCalls();
        }
    }

    private boolean isMicMuted = false;
    private void toggleMicro() {
        isMicMuted = !isMicMuted;
        mLc.muteMic(isMicMuted);
    }

    private boolean isSpeakerEnabled = false;
    private void toggleSpeaker() {
        isSpeakerEnabled = !isSpeakerEnabled;
        mLc.enableSpeaker(isSpeakerEnabled);
    }

    @Override
    public void onDestroy() {
        LinphoneManager.destroy();
    }


    @Override
    public void registrationState(LinphoneCore linphoneCore, LinphoneProxyConfig linphoneProxyConfig, LinphoneCore.RegistrationState registrationState, String s) {
        Log.e(TAG, "registrationState = " + registrationState.toString());
        TextView textView = (TextView)getActivity().findViewById(R.id.state);
        textView.setText(registrationState.toString());
    }

    @Override
    public void globalState(LinphoneCore linphoneCore, LinphoneCore.GlobalState globalState, String s) {
        Log.e(TAG, "globalState = " + globalState.toString());
    }

    @Override
    public void authInfoRequested(LinphoneCore linphoneCore, String s, String s1, String s2) {

    }

    @Override
    public void callStatsUpdated(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCallStats linphoneCallStats) {

    }

    @Override
    public void newSubscriptionRequest(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend, String s) {

    }

    @Override
    public void notifyPresenceReceived(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend) {

    }

    @Override
    public void textReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneAddress linphoneAddress, String s) {

    }

    @Override
    public void dtmfReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, int i) {

    }

    @Override
    public void notifyReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneAddress linphoneAddress, byte[] bytes) {

    }

    @Override
    public void transferState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state) {

    }

    @Override
    public void infoReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneInfoMessage linphoneInfoMessage) {

    }

    @Override
    public void subscriptionStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, SubscriptionState subscriptionState) {

    }

    @Override
    public void publishStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, PublishState publishState) {

    }

    @Override
    public void show(LinphoneCore linphoneCore) {

    }

    @Override
    public void displayStatus(LinphoneCore linphoneCore, String s) {

    }

    @Override
    public void displayMessage(LinphoneCore linphoneCore, String s) {

    }

    @Override
    public void displayWarning(LinphoneCore linphoneCore, String s) {

    }

    @Override
    public void fileTransferProgressIndication(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, int i) {

    }

    @Override
    public void fileTransferRecv(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, byte[] bytes, int i) {

    }

    @Override
    public int fileTransferSend(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, ByteBuffer byteBuffer, int i) {
        return 0;
    }

    @Override
    public void callEncryptionChanged(LinphoneCore linphoneCore, LinphoneCall linphoneCall, boolean b, String s) {

    }

    @Override
    public void isComposingReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom) {

    }

    @Override
    public void uploadProgressIndication(LinphoneCore linphoneCore, int i, int i1) {

    }

    @Override
    public void uploadStateChanged(LinphoneCore linphoneCore, LinphoneCore.LogCollectionUploadState logCollectionUploadState, String s) {

    }

    @Override
    public void messageReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneChatMessage linphoneChatMessage) {

    }

    @Override
    public void notifyReceived(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, String s, LinphoneContent linphoneContent) {

    }

    @Override
    public void configuringStatus(LinphoneCore linphoneCore, LinphoneCore.RemoteProvisioningState remoteProvisioningState, String s) {

    }
}
