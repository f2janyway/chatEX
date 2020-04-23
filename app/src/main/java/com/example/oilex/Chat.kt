package com.example.oilex

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.oilex.retrofit.ApiService
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_chat.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.net.URISyntaxException
import kotlin.random.Random


class Chat : AppCompatActivity() {
    val Tag = "ChatChatChat"
    lateinit var mSocket: Socket
    var username: String? = Random(10000).toString()
    var users: Array<String> = arrayOf()

    var chatList = ArrayList<ChatDto>()
    private lateinit var talkAdapter: TalkAdapter

    lateinit var initDto: ChatDto
    val PICK_IMAGE = 999
    fun sendMsg() {
        val chat = edit_text.text.toString()
        mSocket.emit("say", Gson().toJson(ChatDto(username.toString(), chat, null)))
        edit_text.text.clear()

    }

    lateinit var animButton: Animation
    fun setButtonAnim() {
        animButton = AnimationUtils.loadAnimation(this, R.anim.bounce)
        animButton.interpolator = MyBounceInterpolator(0.2f, 20f)
    }

    class MyBounceInterpolator constructor(val amplitude: Float, val frequency: Float) :
        android.view.animation.Interpolator {
        override fun getInterpolation(input: Float): Float {
            return (-1 * Math.pow(Math.E, (-input / amplitude).toDouble()) *
                    Math.cos((frequency * input).toDouble()) + 1).toFloat()
        }
    }

    //    fun setListener(){
//        val keyListener = View.OnKeyListener (){ v, keyCode, event ->
//            Log.e("hello", keyCode.toString())
//            Log.e("hello", event.toString())
//            true
//        }
//        edit_text.setOnKeyListener(keyListener)
//        Log.e("hello","set Listener")
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        setButtonAnim()
//        setListener()
        username = intent.getStringExtra("username")
        initDto = ChatDto(username.toString(), "님이 입장했습니다.", null)
        chatList.add(initDto)
        talkAdapter = TalkAdapter(chatList,Glide.with(this))
        val manager = LinearLayoutManager(this)
        talkAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                manager.smoothScrollToPosition(
                    chat_recyclerview, null, talkAdapter.itemCount
                )

            }
        })
        chat_recyclerview.apply {
            adapter = talkAdapter
            layoutManager = manager
        }
        try {
            //IO.socket 메소드는 은 저 URL 을 토대로 클라이언트 객체를 Return 합니다.
            mSocket = IO.socket(getString(R.string.server_url))
            Log.e(Tag, mSocket.connected().toString())
            Log.e(Tag, username)
//
        } catch (e: URISyntaxException) {
            Log.e(Tag, e.reason)
        }
//
        edit_text.setOnEditorActionListener { v, actionId, event ->
            Log.e("hello", actionId.toString())
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendMsg()
                    true
                }
                else -> false
            }
        }
        //중요한 것은 아니며 전 액티비티에서 username을 가져왔습니다.
        //원하시는 방법 대로 username을 가져오시면 될 듯 합니다.
        var intent = intent;
//        username = intent.getStringExtra("username")
        // mSocket.connect() 는 socket과 서버를 연결하는 것으로
        // server 측의 io.on('connection',function (socket){-} 을 트리깅합니다.
        // 다시말하자면 mSocket.emit('connection',socket)을 한 것 과 동일하다고 할 수 있습니다.

        // 이제 연결이 성공적으로 되게 되면, server측에서 "connect" event 를 발생시키고
        // 아래코드가 그 event 를 핸들링 합니다. onConnect는 65번째 줄로 가서 살펴 보도록 합니다.
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on("newUser", onNewUser)
        mSocket.on("myMsg", onMyMessage)
        mSocket.on("newMsg", onNewMessage)
        mSocket.on("likeResut", onGetLike)
        mSocket.on("initLike", onInitLike)
        mSocket.on("photo", onPhoto)

        mSocket.connect()


        //send button을 누르면 "say"라는 이벤트를 서버측으로 보낸다.
        send_btn.setOnClickListener {
            sendMsg()
            Log.e(Tag, mSocket.connected().toString())
        }

        likeButton.setOnClickListener {
            val data = likeButton.text
            mSocket.emit("likeClick", data)
        }
        uploadImage_btn.setOnClickListener {
            val mintent = Intent(Intent.ACTION_PICK)
            mintent.apply {
                type = "image/*"
                val mimeType = arrayOf("image/jpeg", "image/png")
                putExtra(Intent.EXTRA_MIME_TYPES, mimeType)
            }
            startActivityForResult(mintent, PICK_IMAGE)
        }


        //logout button을 누르면 "logout"이라는 이벤트를 서버측으로 보낸다.
//        logout.setOnClickListener {
//            mSocket.emit("logout")
//        }
    }

    // onConnect는 Emmiter.Listener를 구현한 인터페이스입니다.
    // 여기서 Server에서 주는 데이터를 어떤식으로 처리할지 정하는 거죠.
    val onConnect: Emitter.Listener = Emitter.Listener {
        //여기서 다시 "login" 이벤트를 서버쪽으로 username 과 함께 보냅니다.
        //서버 측에서는 이 username을 whoIsON Array 에 추가를 할 것입니다.
        Log.e(Tag, "Socket is connected with $username")
        Log.e(Tag + 111, mSocket.connected().toString())

        mSocket.emit("login", Gson().toJson(initDto))


    }


    val onMyMessage = Emitter.Listener {
        Log.e("on", "Mymessage has been triggered.")
        Log.e(Tag, it[0].toString())
        runOnUiThread {
//            chatList.add(Gson().fromJson(it[0].toString(), ChatDto::class.java))
//            talkAdapter.setItem(chatList)
            talkAdapter.addItem(Gson().fromJson(it[0].toString(), ChatDto::class.java))
//            chat_recyclerview.smoothScrollToPosition(talkAdapter.itemCount - 1)
        }
    }

    val onNewMessage = Emitter.Listener {
        Log.e(Tag, "New message has been triggered.")
//        Log.e(Tag, it[0]  + " ")
        runOnUiThread {
//            chatList.add(Gson().fromJson(it[0].toString(), ChatDto::class.java))
//            talkAdapter.setItem(chatList)
            talkAdapter.addItem(Gson().fromJson(it[0].toString(), ChatDto::class.java))
//            chat_recyclerview.smoothScrollToPosition(talkAdapter.itemCount - 1)
        }
    }

    val onInitLike = Emitter.Listener {
        runOnUiThread {
            likeButton.text = it[0].toString()
        }
    }

    val onNewUser: Emitter.Listener = Emitter.Listener {

        var data = it[0] //String으로 넘어옵니다. JSONArray로 넘어오지 않도록 서버에서 코딩한 것 기억나시죠?
        if (data is String) {
            users = data.split(",").toTypedArray() //파싱해줍니다.
            for (a: String in users) {
                Log.e("user", a) //누구누구 있는지 한 번 쫘악 뽑아줘봅시다.
            }
        } else {
            Log.e("error", "Something went wrong")
        }


    }

    val onGetLike = Emitter.Listener {
        val data = it[0]
        runOnUiThread {
            likeButton.text = data.toString()
            likeButton.startAnimation(animButton)
        }
    }
    val onPhoto = Emitter.Listener {
        val uri = it[0]
        val id = it[1]
        Log.e("photo", "$uri + $id +")
        runOnUiThread {

            talkAdapter.addItem(ChatDto(id.toString(), null, uri as String?))
//            chat_recyclerview.smoothScrollToPosition(talkAdapter.itemCount)

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket.emit("logout", Gson().toJson(ChatDto(username.toString(), "님이 나갔습니다.", null)))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {

            getRealPathFromURI(data?.data)
        }
    }

    private fun getRealPathFromURI(contentURI: Uri?) {

        var file: String?
        if ("content" == contentURI!!.scheme) {
            val cursor: Cursor? = contentResolver.query(
                contentURI,
                arrayOf(MediaStore.Images.ImageColumns.DATA),
                null,
                null,
                null
            )
            cursor?.moveToFirst();
            file = cursor?.getString(0)
            cursor?.close()
        } else {
            file = contentURI.path
        }
        Log.e("Chosen path ", "Chosen path = $file");
//        return file
        uploadToServer(file)
    }

    fun uploadToServer(path: String?) {
        val service = ApiService.retrofit.create(ApiService::class.java)
        val file = File(path)
        Log.e("file path", "" + file + " <-path , name->" + file.name)
        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val part = MultipartBody.Part.createFormData("img", file.name, requestFile)
        val id = RequestBody.create(MediaType.parse("text/plain"), username)
//        val name= RequestBody.create(MediaType.parse("text/plain"), "name")
        service.postImage(part, id).enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@Chat, "upload fail", Toast.LENGTH_SHORT)
                    .show()
                Log.e("fail", call.request().toString())
            }

            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                Toast.makeText(this@Chat, "upload success", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }
}
