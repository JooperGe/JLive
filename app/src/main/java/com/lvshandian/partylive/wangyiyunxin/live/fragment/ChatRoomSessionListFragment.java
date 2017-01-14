package com.lvshandian.partylive.wangyiyunxin.live.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lvshandian.partylive.R;
import com.lvshandian.partylive.moudles.start.LoginActivity;
import com.lvshandian.partylive.moudles.start.LogoutHelper;
import com.lvshandian.partylive.utils.LogUtils;
import com.lvshandian.partylive.wangyiyunxin.config.preference.Preferences;
import com.lvshandian.partylive.wangyiyunxin.main.fragment.MainTabFragment;
import com.lvshandian.partylive.wangyiyunxin.main.reminder.ReminderManager;
import com.lvshandian.partylive.wangyiyunxin.session.SessionHelper;
import com.lvshandian.partylive.wangyiyunxin.session.extension.GuessAttachment;
import com.lvshandian.partylive.wangyiyunxin.session.extension.RTSAttachment;
import com.lvshandian.partylive.wangyiyunxin.session.extension.SnapChatAttachment;
import com.lvshandian.partylive.wangyiyunxin.session.extension.StickerAttachment;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.util.log.LogUtil;
import com.netease.nim.uikit.recent.RecentContactsCallback;
import com.netease.nim.uikit.recent.RecentContactsFragment;
import com.netease.nim.uikit.session.constant.Extras;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.StatusCode;
import com.netease.nimlib.sdk.auth.AuthServiceObserver;
import com.netease.nimlib.sdk.auth.ClientType;
import com.netease.nimlib.sdk.auth.OnlineClient;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.attachment.MsgAttachment;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.zhy.autolayout.AutoRelativeLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by sll on 2016/11/28.
 */

/**
 * 直播消息中心
 *
 * @author sll
 * @time 2016/11/28 18:37
 */
public class ChatRoomSessionListFragment extends MainTabFragment {
    AutoRelativeLayout chatMessagesListLayout;
    ImageView sessionFragmentX;
    private View notifyBar;

    private TextView notifyBarText;

    // 同时在线的其他端的信息
    private List<OnlineClient> onlineClients;

    private View multiportBar;

    private RecentContactsFragment fragment;
    private FragmentManager fragmentManager;
    private LiveMessageFragment messageFragment;

    public ChatRoomSessionListFragment() {
        this.setContainerId(0);
    }

    public void init(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    /**
     * 隐藏此fragment
     *
     * @author sll
     * @time 2016/12/14 14:29
     */
    public void hide() {
        if (messageFragment != null)
            messageFragment.hide();
        fragmentManager.popBackStack();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        onCurrent();
    }

    @Override
    public void onDestroy() {
        registerObservers(false);
        super.onDestroy();
    }

    @Override
    protected void onInit() {
        findViews();
        registerObservers(true);

        addRecentContactsFragment();
    }

    private void registerObservers(boolean register) {
        NIMClient.getService(AuthServiceObserver.class).observeOtherClients(clientsObserver, register);
        NIMClient.getService(AuthServiceObserver.class).observeOnlineStatus(userStatusObserver, register);
    }

    private void findViews() {
        notifyBar = getView().findViewById(R.id.status_notify_bar);
        notifyBarText = (TextView) getView().findViewById(R.id.status_desc_label);
        notifyBar.setVisibility(View.GONE);

        multiportBar = getView().findViewById(R.id.multiport_notify_bar);
        multiportBar.setVisibility(View.GONE);
        multiportBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                MultiportActivity.startActivity(getActivity(), onlineClients);
            }
        });
        LogUtils.i("WangYi", "getView():" + getView().getId());
        LogUtils.i("WangYi", "layout:" + R.layout.chat_room_session_list);
        chatMessagesListLayout = (AutoRelativeLayout) getView().findViewById(R.id.chat_messages_list_layout);
        chatMessagesListLayout.setVisibility(View.VISIBLE);
        sessionFragmentX = (ImageView) getView().findViewById(R.id.session_fragment_x);

        sessionFragmentX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragmentManager.popBackStack();
            }
        });
    }

    /**
     * 用户状态变化
     */
    Observer<StatusCode> userStatusObserver = new Observer<StatusCode>() {

        @Override
        public void onEvent(StatusCode code) {
            if (code.wontAutoLogin()) {
                kickOut(code);
            } else {
                if (code == StatusCode.NET_BROKEN) {
                    notifyBar.setVisibility(View.VISIBLE);
                    notifyBarText.setText(R.string.net_broken);
                } else if (code == StatusCode.UNLOGIN) {
                    notifyBar.setVisibility(View.VISIBLE);
                    notifyBarText.setText(R.string.nim_status_unlogin);
                } else if (code == StatusCode.CONNECTING) {
                    notifyBar.setVisibility(View.VISIBLE);
                    notifyBarText.setText(R.string.nim_status_connecting);
                } else if (code == StatusCode.LOGINING) {
                    notifyBar.setVisibility(View.VISIBLE);
                    notifyBarText.setText(R.string.nim_status_logining);
                } else {
                    notifyBar.setVisibility(View.GONE);
                }
            }
        }
    };

    Observer<List<OnlineClient>> clientsObserver = new Observer<List<OnlineClient>>() {
        @Override
        public void onEvent(List<OnlineClient> onlineClients) {
            ChatRoomSessionListFragment.this.onlineClients = onlineClients;
            if (onlineClients == null || onlineClients.size() == 0) {
                multiportBar.setVisibility(View.GONE);
            } else {
                multiportBar.setVisibility(View.VISIBLE);
                TextView status = (TextView) multiportBar.findViewById(R.id.multiport_desc_label);
                OnlineClient client = onlineClients.get(0);
                switch (client.getClientType()) {
                    case ClientType.Windows:
                        status.setText(getString(R.string.multiport_logging) + getString(R.string.computer_version));
                        break;
                    case ClientType.Web:
                        status.setText(getString(R.string.multiport_logging) + getString(R.string.web_version));
                        break;
                    case ClientType.iOS:
                    case ClientType.Android:
                        status.setText(getString(R.string.multiport_logging) + getString(R.string.mobile_version));
                        break;
                    default:
                        multiportBar.setVisibility(View.GONE);
                        break;
                }
            }
        }
    };

    private void kickOut(StatusCode code) {
        Preferences.saveUserToken("");

        if (code == StatusCode.PWD_ERROR) {
            LogUtil.e("Auth", "user password error");
            Toast.makeText(getActivity(), R.string.login_failed, Toast.LENGTH_SHORT).show();
        } else {
            LogUtil.i("Auth", "Kicked!");
        }
        onLogout();
    }

    // 注销
    private void onLogout() {
        // 清理缓存&注销监听&清除状态
        LogoutHelper.logout();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(LoginActivity.KICK_OUT, true);
        getActivity().startActivity(intent);
//        LoginActivity.start(getActivity(), true);
        getActivity().finish();
    }


    // 将最近联系人列表fragment动态集成进来。 开发者也可以使用在xml中配置的方式静态集成。
    private void addRecentContactsFragment() {
//        fragment = (RecentContactsFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.session_recentContactsFragment);
        fragment = new RecentContactsFragment();
        fragment.setContainerId(R.id.messages_fragment);
        fragmentManager.beginTransaction().replace(R.id.session_recentContactsFragment, fragment).commit();
//        final UI activity = (UI) getActivity();

        // 如果是activity从堆栈恢复，FM中已经存在恢复而来的fragment，此时会使用恢复来的，而new出来这个会被丢弃掉
//        fragment = (RecentContactsFragment) activity.addFragment(fragment);

        fragment.setCallback(new RecentContactsCallback() {
            @Override
            public void onRecentContactsLoaded() {
                // 最近联系人列表加载完毕
            }

            @Override
            public void onUnreadCountChange(int unreadCount) {
                ReminderManager.getInstance().updateSessionUnreadNum(unreadCount);
                LogUtils.i("WangYi", "未读数:" + unreadCount);
            }

            @Override
            public void onItemClick(RecentContact recent) {
                // 回调函数，以供打开会话窗口时传入定制化参数，或者做其他动作
                switch (recent.getSessionType()) {
                    case P2P:
//                        SessionHelper.startP2PSession(getActivity(), recent.getContactId());
                        Intent intent = new Intent();
                        intent.putExtra(Extras.EXTRA_ACCOUNT, recent.getContactId());
                        intent.putExtra("SESSION_NAME", recent.getFromNick());
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Bundle arguments = intent.getExtras();
                        arguments.putSerializable(Extras.EXTRA_TYPE, SessionTypeEnum.P2P);
                        messageFragment = new LiveMessageFragment();
                        messageFragment.setArguments(arguments);
                        fragmentManager.beginTransaction().add(R.id.watch_room_message_fragment, messageFragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).addToBackStack(null).commit();
                        break;
                    case Team:
//                        SessionHelper.startTeamSession(getActivity(), recent.getContactId());
                        Intent intent1 = new Intent();
                        intent1.putExtra(Extras.EXTRA_ACCOUNT, recent.getContactId());
                        intent1.putExtra("SESSION_NAME", "");
                        intent1.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Bundle arguments1 = intent1.getExtras();
                        arguments1.putSerializable(Extras.EXTRA_TYPE, SessionTypeEnum.Team);
                        messageFragment = new LiveMessageFragment();
                        messageFragment.setArguments(arguments1);
                        fragmentManager.beginTransaction().add(R.id.watch_room_message_fragment, messageFragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).addToBackStack(null).commit();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public String getDigestOfAttachment(MsgAttachment attachment) {
                // 设置自定义消息的摘要消息，展示在最近联系人列表的消息缩略栏上
                // 当然，你也可以自定义一些内建消息的缩略语，例如图片，语音，音视频会话等，自定义的缩略语会被优先使用。
                if (attachment instanceof GuessAttachment) {
                    GuessAttachment guess = (GuessAttachment) attachment;
                    return guess.getValue().getDesc();
                } else if (attachment instanceof RTSAttachment) {
                    return "[白板]";
                } else if (attachment instanceof StickerAttachment) {
                    return "[贴图]";
                } else if (attachment instanceof SnapChatAttachment) {
                    return "[阅后即焚]";
                }

                return null;
            }

            @Override
            public String getDigestOfTipMsg(RecentContact recent) {
                String msgId = recent.getRecentMessageId();
                List<String> uuids = new ArrayList<>(1);
                uuids.add(msgId);
                List<IMMessage> msgs = NIMClient.getService(MsgService.class).queryMessageListByUuidBlock(uuids);
                if (msgs != null && !msgs.isEmpty()) {
                    IMMessage msg = msgs.get(0);
                    Map<String, Object> content = msg.getRemoteExtension();
                    if (content != null && !content.isEmpty()) {
                        return (String) content.get("content");
                    }
                }

                return null;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO: inflate a fragment view
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}