package com.example.oilex

import android.content.Context
import com.kakao.auth.AuthType
import com.kakao.auth.Session
import com.kakao.usermgmt.LoginButton
import java.lang.Exception

class KakaoLogin(context: Context) : LoginButton(context){

    fun call(){
        try{
            val methodAuthTypes = LoginButton::class.java.getDeclaredMethod("getAuthTypes")
            methodAuthTypes.isAccessible = true
            val authTypesList:List<AuthType> = methodAuthTypes.invoke(this) as List<AuthType>
            if(authTypesList.size== 1){
                Session.getCurrentSession().open(authTypesList[0],context as MainActivity)
            }else{
                val methodOnClickButton =
                    LoginButton::class.java.getDeclaredMethod("onClickLoginBtton",List::class.java)
                methodOnClickButton.isAccessible = true
                methodOnClickButton.invoke(this,authTypesList)
            }
        }catch (e : Exception){
            Session.getCurrentSession().open(AuthType.KAKAO_ACCOUNT, context as (MainActivity))
        }


    }

}