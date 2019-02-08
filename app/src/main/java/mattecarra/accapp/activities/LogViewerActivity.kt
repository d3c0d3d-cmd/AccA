package mattecarra.accapp.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import mattecarra.accapp.R
import mattecarra.accapp.adapters.LogRecyclerViewAdapter
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.*

class LogViewerActivity : AppCompatActivity() {
    private val LOG_TAG = "LogViewerActivity"

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LogRecyclerViewAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var file: File
    private var onBottom = true

    private val handler = Handler()
    private val readLogFile = object : Runnable {
        override fun run() {
            Log.d(LOG_TAG, "Reading log file...")
            val r = this //need this to make it recursive
            doAsync {
                val newList = readLogs()
                uiThread {
                    if(newList.size < adapter.size()) {
                        adapter.setList(newList)
                    } else if(newList.size > adapter.size()) {
                        for (i in adapter.size() until newList.size) {
                            adapter.add(newList[i])
                        }
                    } else {
                        adapter.add("test")
                    }

                    if (onBottom) {
                        scrollToBottom()
                    }

                    handler.postDelayed(r, 1000)// Repeat the same runnable code block again after 1 seconds
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun readLogs(): List<String> {
        return file.readText(charset = Charsets.UTF_8).split("\n")
    }

    private fun scrollToBottom() {
        recyclerView.scrollToPosition(adapter.itemCount - 1)
    }

    private val clickerListener: (String) -> Unit = { line: String ->
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("log line", line)
        clipboard.primaryClip = clip
        Toast.makeText(this, R.string.text_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        this.adapter.saveState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val regexp = """acc-daemon-(.+)\.log""".toRegex()
        val logsDir = File(Environment.getExternalStorageDirectory(), "acc/logs/")
        val file = logsDir.listFiles().find {
            regexp.matches(it.name)
        }

        if(file?.exists() != true) {
            val dialog = MaterialDialog(this).show {
                title(R.string.log_file_not_found_title)
                message(R.string.log_file_not_found_title)
                positiveButton(android.R.string.ok) {
                    finish()
                }
                cancelOnTouchOutside(false)
            }
            dialog.setOnKeyListener { _, keyCode, _ ->
                if(keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss()
                    finish()
                    false
                } else true
            }
            return
        }

        this.file = file

        supportActionBar?.title = getString(R.string.title_activity_log_view_file_name, this.file.name)

        recyclerView = findViewById<View>(R.id.log_recycler) as RecyclerView
        linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        adapter = LogRecyclerViewAdapter(ArrayList(readLogs()), clickerListener)
        if (savedInstanceState != null) {
            this.adapter.restoreState(savedInstanceState)
        }
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        linearLayoutManager.stackFromEnd = true

        this.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy == 0) {
                    return
                }
                if (dy < 0) {
                    onBottom = false
                } else if (!onBottom) {
                    if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == adapter.itemCount - 1) {
                        onBottom = true
                    }
                }
            }
        })
    }

    override fun onResume() {
        Log.d(LOG_TAG, "onResume")
        handler.post(readLogFile) // Start the initial runnable task by posting through the handler

        super.onResume()
    }

    override fun onPause() {
        Log.d(LOG_TAG, "onPause")
        handler.removeCallbacks(readLogFile)

        super.onPause()
    }
}