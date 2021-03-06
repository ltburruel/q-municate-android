package com.quickblox.qmunicate.qb.commands;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.quickblox.module.users.model.QBUser;
import com.quickblox.qmunicate.App;
import com.quickblox.qmunicate.R;
import com.quickblox.qmunicate.core.command.ServiceCommand;
import com.quickblox.qmunicate.model.AppSession;
import com.quickblox.qmunicate.model.LoginType;
import com.quickblox.qmunicate.qb.helpers.QBAuthHelper;
import com.quickblox.qmunicate.service.QBService;
import com.quickblox.qmunicate.service.QBServiceConsts;
import com.quickblox.qmunicate.utils.PrefsHelper;

public class QBLoginRestWithSocialCommand extends ServiceCommand {

    private static final String TAG = QBLoginRestWithSocialCommand.class.getSimpleName();

    private final QBAuthHelper authHelper;

    public QBLoginRestWithSocialCommand(Context context, QBAuthHelper authHelper, String successAction,
            String failAction) {
        super(context, successAction, failAction);
        this.authHelper = authHelper;
    }

    public static void start(Context context, String socialProvier, String accessToken,
            String accessTokenSecret) {
        Intent intent = new Intent(QBServiceConsts.SOCIAL_LOGIN_ACTION, null, context, QBService.class);
        intent.putExtra(QBServiceConsts.EXTRA_SOCIAL_PROVIDER, socialProvier);
        intent.putExtra(QBServiceConsts.EXTRA_ACCESS_TOKEN, accessToken);
        intent.putExtra(QBServiceConsts.EXTRA_ACCESS_TOKEN_SECRET, accessTokenSecret);
        context.startService(intent);
    }

    @Override
    public Bundle perform(Bundle extras) throws Exception {
        String socialProvider = (String) extras.getSerializable(QBServiceConsts.EXTRA_SOCIAL_PROVIDER);
        String accessToken = (String) extras.getSerializable(QBServiceConsts.EXTRA_ACCESS_TOKEN);
        String accessTokenSecret = (String) extras.getSerializable(QBServiceConsts.EXTRA_ACCESS_TOKEN_SECRET);
        QBUser user = authHelper.login(socialProvider, accessToken, accessTokenSecret);
        if(TextUtils.isEmpty(user.getWebsite())) {
            QBUser newUser = new QBUser();
            newUser.setId(user.getId());
            newUser.setPassword(user.getPassword());
            newUser.setWebsite(context.getString(R.string.inf_url_to_facebook_avatar, user.getFacebookId()));
            App.getInstance().getPrefsHelper().savePref(PrefsHelper.PREF_IMPORT_INITIALIZED, false);
            extras.putSerializable(QBServiceConsts.EXTRA_USER, newUser);
        } else {
            App.getInstance().getPrefsHelper().savePref(PrefsHelper.PREF_IMPORT_INITIALIZED, true);
            extras.putSerializable(QBServiceConsts.EXTRA_USER, user);
        }
        return extras;
    }
}