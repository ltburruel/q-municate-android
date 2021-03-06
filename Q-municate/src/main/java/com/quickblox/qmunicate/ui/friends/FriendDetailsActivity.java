package com.quickblox.qmunicate.ui.friends;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.quickblox.module.chat.model.QBDialog;
import com.quickblox.module.chat.model.QBDialogType;
import com.quickblox.module.videochat_webrtc.WebRTC;
import com.quickblox.qmunicate.R;
import com.quickblox.qmunicate.caching.DatabaseManager;
import com.quickblox.qmunicate.core.command.Command;
import com.quickblox.qmunicate.model.AppSession;
import com.quickblox.qmunicate.model.Friend;
import com.quickblox.qmunicate.qb.commands.QBCreatePrivateChatCommand;
import com.quickblox.qmunicate.qb.commands.QBRemoveFriendCommand;
import com.quickblox.qmunicate.service.QBServiceConsts;
import com.quickblox.qmunicate.ui.base.BaseLogeableActivity;
import com.quickblox.qmunicate.ui.chats.PrivateDialogActivity;
import com.quickblox.qmunicate.ui.dialogs.ConfirmDialog;
import com.quickblox.qmunicate.ui.mediacall.CallActivity;
import com.quickblox.qmunicate.ui.views.RoundedImageView;
import com.quickblox.qmunicate.utils.Consts;
import com.quickblox.qmunicate.utils.DialogUtils;
import com.quickblox.qmunicate.utils.ErrorUtils;

import java.util.List;

public class FriendDetailsActivity extends BaseLogeableActivity {

    private RoundedImageView avatarImageView;
    private TextView nameTextView;
    private TextView statusTextView;
    private ImageView onlineImageView;
    private TextView onlineStatusTextView;
    private TextView phoneTextView;
    private View phoneView;

    private Friend friend;

    public static void start(Context context, Friend friend) {
        Intent intent = new Intent(context, FriendDetailsActivity.class);
        intent.putExtra(QBServiceConsts.EXTRA_FRIEND, friend);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_details);
        canPerformLogout.set(true);
        friend = (Friend) getIntent().getExtras().getSerializable(QBServiceConsts.EXTRA_FRIEND);
        initUI();
        initUIWithFriendsData();
        initBroadcastActionList();
    }

    private void initUI() {
        avatarImageView = _findViewById(R.id.avatar_imageview);
        avatarImageView.setOval(true);
        nameTextView = _findViewById(R.id.name_textview);
        statusTextView = _findViewById(R.id.status_textview);
        onlineImageView = _findViewById(R.id.online_imageview);
        onlineStatusTextView = _findViewById(R.id.online_status_textview);
        phoneTextView = _findViewById(R.id.phone_textview);
        phoneView = _findViewById(R.id.phone_relativelayout);
    }

    private void initBroadcastActionList() {
        addAction(QBServiceConsts.REMOVE_FRIEND_SUCCESS_ACTION, new RemoveFriendSuccessAction());
        addAction(QBServiceConsts.REMOVE_FRIEND_FAIL_ACTION, failAction);
        addAction(QBServiceConsts.GET_FILE_FAIL_ACTION, failAction);
        addAction(QBServiceConsts.GET_FILE_FAIL_ACTION, failAction);
        addAction(QBServiceConsts.CREATE_PRIVATE_CHAT_SUCCESS_ACTION, new CreateChatSuccessAction());
        addAction(QBServiceConsts.CREATE_PRIVATE_CHAT_FAIL_ACTION, failAction);
    }

    private void initUIWithFriendsData() {
        loadAvatar();
        setName();
        setOnlineStatus();
        setStatus();
        setPhone();
    }

    private void setStatus() {
        statusTextView.setText(friend.getStatus());
    }

    private void setName() {
        nameTextView.setText(friend.getFullname());
    }

    private void setPhone() {
        if (friend.getPhone() != null) {
            phoneView.setVisibility(View.VISIBLE);
        } else {
            phoneView.setVisibility(View.GONE);
        }
        phoneTextView.setText(friend.getPhone());
    }

    private void setOnlineStatus() {
        if (friend.isOnline()) {
            onlineImageView.setVisibility(View.VISIBLE);
        } else {
            onlineImageView.setVisibility(View.GONE);
        }
        onlineStatusTextView.setText(friend.getOnlineStatus());
    }

    private void loadAvatar() {
        String url = friend.getAvatarUrl();
        ImageLoader.getInstance().displayImage(url, avatarImageView, Consts.UIL_AVATAR_DISPLAY_OPTIONS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.friend_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateToParent();
                return true;
            //TODO implement
            //            case R.id.action_delete:
            //                showRemoveUserDialog();
            //                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showRemoveUserDialog() {
        ConfirmDialog dialog = ConfirmDialog.newInstance(R.string.dlg_remove_user, R.string.dlg_confirm);
        dialog.setPositiveButton(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showProgress();
                QBRemoveFriendCommand.start(FriendDetailsActivity.this, friend);
            }
        });
        dialog.show(getFragmentManager(), null);
    }

    public void videoCallClickListener(View view) {
        callToUser(friend, WebRTC.MEDIA_STREAM.VIDEO);
    }

    private void callToUser(Friend friend, WebRTC.MEDIA_STREAM callType) {
        if (friend.getId() != AppSession.getSession().getUser().getId()) {
            CallActivity.start(FriendDetailsActivity.this, friend, callType);
        }
    }

    public void voiceCallClickListener(View view) {
        callToUser(friend, WebRTC.MEDIA_STREAM.AUDIO);
    }

    public void chatClickListener(View view) {
        QBDialog existingPrivateDialog = getExistPrivateDialog(friend.getId());
        if (existingPrivateDialog != null) {
            PrivateDialogActivity.start(FriendDetailsActivity.this, friend, existingPrivateDialog);
        } else {
            QBCreatePrivateChatCommand.start(this, friend);
        }
    }

    private QBDialog getExistPrivateDialog(int opponentId) {
        List<QBDialog> dialogList = DatabaseManager.getDialogsByOpponent(this, opponentId,
                QBDialogType.PRIVATE);
        if (!dialogList.isEmpty()) {
            return dialogList.get(0);
        } else {
            return null;
        }
    }

    private class CreateChatSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) throws Exception {
            QBDialog dialog = (QBDialog) bundle.getSerializable(QBServiceConsts.EXTRA_DIALOG);
            if (dialog != null) {
                PrivateDialogActivity.start(FriendDetailsActivity.this, friend, dialog);
                finish();
            } else {
                ErrorUtils.showError(FriendDetailsActivity.this, getString(R.string.dlg_fail_create_chat));
            }
        }
    }

    private class RemoveFriendSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) {
            DialogUtils.showLong(FriendDetailsActivity.this, getString(R.string.dlg_friend_removed));
            finish();
        }
    }
}