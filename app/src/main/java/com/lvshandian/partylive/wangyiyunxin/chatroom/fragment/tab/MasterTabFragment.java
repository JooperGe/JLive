package com.lvshandian.partylive.wangyiyunxin.chatroom.fragment.tab;


import com.lvshandian.partylive.R;
import com.lvshandian.partylive.wangyiyunxin.chatroom.fragment.MasterFragment;

/**
 * 主播基类fragment
 * Created by hzxuwen on 2015/12/14.
 */
public class MasterTabFragment extends ChatRoomTabFragment {
    private MasterFragment fragment;
    @Override
    protected void onInit() {
        fragment = (MasterFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.master_fragment);
    }

    @Override
    public void onCurrent() {
        super.onCurrent();
        if (fragment != null) {
            fragment.onCurrent();
        }
    }
}
