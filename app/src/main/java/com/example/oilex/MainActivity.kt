package com.example.oilex

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.kakao.auth.ISessionCallback
import com.kakao.auth.Session
import com.kakao.network.ErrorResult
import com.kakao.usermgmt.UserManagement
import com.kakao.usermgmt.callback.LogoutResponseCallback
import com.kakao.usermgmt.callback.MeV2ResponseCallback
import com.kakao.usermgmt.response.MeV2Response
import com.kakao.util.exception.KakaoException
import com.kakao.util.helper.Utility.getPackageInfo
import com.kakao.util.helper.log.Logger
import kotlinx.android.synthetic.main.activity_main.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.Executor


class MainActivity : AppCompatActivity() {

    private var callback: SessionCallback? = null
    lateinit var kakaoBtn: MutableLiveData<String>
    lateinit var idText: MutableLiveData<String>

    var isSessionOpend = false

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        chatroom_btn.setOnClickListener {
            val intent = Intent(this, Chat::class.java)
            val dialogB = AlertDialog.Builder(this)
            dialogB.apply {
                val editText = EditText(this@MainActivity)
                title = "로그인"
                setMessage("닉네임을 적어주세요!!")
                setView(editText)
                setPositiveButton("확인") { dialog, which ->
                    intent.putExtra("username", editText.text.toString())
                    startActivity(intent)
                }
            }
            val dialog = dialogB.create()
            dialog.show()
        }
        //kakao
        isSessionOpend = Session.getCurrentSession().isOpened
        kakaoBtn = MutableLiveData()
        idText = MutableLiveData()
        if (isSessionOpend) {
            kakaoBtn.value = "log out"
            Log.e("appcache", Session.getCurrentSession().appCache.toString())
        } else {
            kakaoBtn.value = "log in"
        }

        kakaoBtn.observe(this, Observer {
            com_kakao_login.text = it
        })
        idText.observe(this, Observer {
            user_id_textvView.text = it
        })
        com_kakao_login.setOnClickListener {
            changeButton()
        }

        //biometricPrompt
        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.e("MY_APP_TAG", "App can authenticate using biometrics.")
                executor = ContextCompat.getMainExecutor(this)
                biometricPrompt = BiometricPrompt(
                    this,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(
                                    applicationContext,
                                    "Authentication error: $errString", Toast.LENGTH_SHORT
                                )
                                .show()
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            Toast.makeText(
                                    applicationContext,
                                    "Authentication succeeded!", Toast.LENGTH_SHORT
                                )
                                .show()

                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Toast.makeText(
                                    applicationContext, "Authentication failed",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    })

                promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("지문 인증")
                    .setSubtitle("지문 센서에 입력해 주세요")
                    .setNegativeButtonText("Use account password")
                    .setConfirmationRequired(false)
                    .build()

                biometric_login_btn.setOnClickListener {
                    biometricPrompt.authenticate(promptInfo)
                }

            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Log.e("MY_APP_TAG", "No biometric features available on this device.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Log.e("MY_APP_TAG", "Biometric features are currently unavailable.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                Log.e(
                    "MY_APP_TAG", "The user hasn't associated " +
                            "any biometric credentials with their account."
                )
        }

    }

    override fun onDestroy() {
        super.onDestroy()
//        logoutSession()
        Log.e(
            "myLog onDestroy",
            "Session.getCurrentSession().isOpened ${Session.getCurrentSession().isOpened} "
        )
    }

    //abuot Kakao login
    fun changeButton() {
        when (kakaoBtn.value) {
            "log in" -> {
                openCallback()
                KakaoLogin(this).call()
                kakaoBtn.value = "log out"
            }
            "log out" -> {
                logoutSession()
                kakaoBtn.value = "log in"
            }
        }
    }

    inner class SessionCallback : ISessionCallback {
        override fun onSessionOpened() {
            Log.e("myLog", "onSessionOpened " + "onSessionOpened")
            Log.e(
                "myLog onSessionOpened",
                "Session.getCurrentSession().isOpened ${Session.getCurrentSession().isOpened} "
            )
            redirectSignupActivity()
        }

        override fun onSessionOpenFailed(exception: KakaoException) {
            Log.e("myLog", "onSessionOpenFailed " + "onSessionOpenFailed")
            if (exception != null) {
                Logger.e(exception)
            }
        }
    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        Log.e("myLog", "onActivityResult11 " + "onActivityResult11")
        if (Session.getCurrentSession().handleActivityResult(
                requestCode,
                resultCode, data
            )
        ) {
            Log.e("myLog", "onActivityResult onActivityResult $data")
            Log.e(
                "myLog onActivityResult",
                "Session.getCurrentSession().isOpened ${Session.getCurrentSession().isOpened} "
            )

            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun openCallback() {
        Log.e("!!!!!!!!!!!", "opencall")
        callback = SessionCallback()
        Session.getCurrentSession().addCallback(callback)
        Session.getCurrentSession().checkAndImplicitOpen()
    }

    fun logoutSession() {
        idText.value = "로그인이 필요합니당"
        UserManagement.getInstance().requestLogout(object : LogoutResponseCallback() {
            override fun onCompleteLogout() {
                Log.e(
                    "myLog !!!!!!",
                    "Session.getCurrentSession().isOpened ${Session.getCurrentSession().isOpened} "
                )
            }
        })
        Session.getCurrentSession().removeCallback(callback)
    }


    fun redirectSignupActivity() {
        Log.e("myLog", "redirectSignupActivity " + "redirectSignupActivity")
        requestMe()

        // final Intent intent = new Intent(this, SampleSignupActivity.class);

        // startActivity(intent);

        // finish();
    }


    private fun requestMe() {
        UserManagement.getInstance().me(object : MeV2ResponseCallback() {
            override fun onSuccess(result: MeV2Response?) {
//                Log.e("myLog", "userProfile" + result?.id)
                Log.e("myLog", "userProfile" + result?.nickname)
//                Log.e(
//                    "myLog",
//                    "userProfile" + result?.thumbnailImagePath
//                )
                idText.value = result?.kakaoAccount?.email ?: result?.nickname
//                user_id_textvView.text = idText.value
//                Log.e("myLogkakaoAccount",result?.kakaoAccount.toString())
//                Log.e("myLogphoneNumber",result?.kakaoAccount?.phoneNumber.toString())
                Log.e("myLogemail", result?.kakaoAccount?.email.toString())
//                Log.e("myLogproperties",result?.properties.toString())
                Log.e("myLogprofileImagePath", result?.kakaoAccount.toString())
                Log.e("myLogthymbnailImagePath", result?.thumbnailImagePath.toString())
                Log.e("myLogAgeRange", result?.kakaoAccount?.ageRange.toString())
                Log.e("myLogId", result?.id.toString())

            }

            override fun onFailure(errorResult: ErrorResult) {
                val message = "failed to get user info. msg=$errorResult"
            }

            override fun onSessionClosed(errorResult: ErrorResult) {}

        })
    }

    ///음....이거랑 cmd 에서 뽑은거랑 왜 다른가...
    fun getKeyHash(context: Context?): String? {
        val packageInfo: PackageInfo = getPackageInfo(context, PackageManager.GET_SIGNATURES)
            ?: return null
        for (signature in packageInfo.signatures) {
            try {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                return Base64.encodeToString(md.digest(), Base64.NO_WRAP)
            } catch (e: NoSuchAlgorithmException) {
                Log.w(
                    "signature",
                    "Unable to get MessageDigest. signature=$signature",
                    e
                )
            }
        }
        return null
    }
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        //barcode implement
//        val text = "jyg"
//        val multiFormatWriter = MultiFormatWriter()
//        val bitMatrix =
//            multiFormatWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300)
//        val barcodeEncoder = BarcodeEncoder()
//        val bitmap = barcodeEncoder.createBitmap(bitMatrix)
//        barcord_imageView.setImageBitmap(bitmap)
//
//        /**
//         * 로그인 버튼을 클릭 했을시 access token을 요청하도록 설정한다.
//         *
//         * @param savedInstanceState 기존 session 정보가 저장된 객체
//         */
//        callback = SessionCallback()
//        Session.getCurrentSession().addCallback(callback)
//        Session.getCurrentSession().checkAndImplicitOpen()
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if(Session.getCurrentSession().handleActivityResult(requestCode,resultCode,data))
//            return
//        super.onActivityResult(requestCode, resultCode, data)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        Session.getCurrentSession().removeCallback(callback)
//    }
//
//
//    inner class SessionCallback : ISessionCallback {
//        override fun onSessionOpened() {
//            requestMe()
//            UserManagement.getInstance().me(object : MeV2ResponseCallback(){
//                override fun onSuccess(result: MeV2Response?) {
//                    Log.e("카카오onSuccess",result.toString())
//                    Log.e("카카오id", result?.id.toString())
//                    Log.e("카카오nickname",result?.nickname.toString())
//                    Log.e("카카오kakaoAccount",result?.kakaoAccount.toString())
//                    Log.e("카카오properties",result?.properties.toString())
//                    Log.e("카카오profileImagePath",result?.profileImagePath.toString())
//                }
//
//                override fun onSessionClosed(errorResult: ErrorResult?) {
//                    Log.e("실패","....")
//                }
//            })
////            redirectSignupActivity()
//            }
//
//        override fun onSessionOpenFailed(exception: KakaoException) {
//            if (exception != null) {
//                Logger.e(exception)
//            }
//        }
//
//    }
//
//    fun redirectSignupActivity() {
//        val intent = Intent(this, MainActivity::class.java)
//        startActivity(intent)
//        finish()
//    }
//    private fun requestMe() {
//        UserManagement.getInstance().me(object : MeV2ResponseCallback() {
//            override fun onFailure(errorResult: ErrorResult) {
//                val message = "failed to get user info. msg=$errorResult"
//                Log.e("카카오",message)
//            }
//
//            override fun onSessionClosed(errorResult: ErrorResult) {
//                redirectSignupActivity()
//            }
//
//            override fun onSuccess(response: MeV2Response) {
//                Log.e("카카오","user id : " + response.id)
//                val kakaoAccount: UserAccount = response.kakaoAccount
//                val email = kakaoAccount.email
//                when {
//                    email != null -> {
//                        Log.e("카카오","email: $email")
//                    }
//                    kakaoAccount.needsScopeAccountEmail() -> {
//                        // 동의 요청 후 이메일 획득 가능
//                        // 단, 선택 동의로 설정되어 있다면 서비스 이용 시나리오 상에서 반드시 필요한 경우에만 요청해야 합니다.
//                    }
//                    else -> {
//                        // 이메일 획득 불가
//                    }
//                }
//                Log.e("카카오","nickname: " + response.nickname)
//                Log.e("카카오","profile image: " + response.profileImagePath)
//                Log.e("카카오","thumbnail image: " + response.thumbnailImagePath)
//                redirectSignupActivity()
//            }
//
//
//        })
//    }
}
