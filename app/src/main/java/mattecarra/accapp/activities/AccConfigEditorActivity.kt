package mattecarra.accapp.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import kotlinx.android.synthetic.main.content_acc_config_editor.*
import mattecarra.accapp.utils.AccUtils
import mattecarra.accapp.R
import android.app.Activity
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip
import mattecarra.accapp.models.AccConfig
import mattecarra.accapp.utils.Constants
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


class AccConfigEditorActivity : AppCompatActivity(), NumberPicker.OnValueChangeListener, CompoundButton.OnCheckedChangeListener {

    private var unsavedChanges = false
    private lateinit var mAccConfig: AccConfig

    private fun returnResults() {
        val returnIntent = Intent()
        returnIntent.putExtra(Constants.DATA_KEY, intent.getBundleExtra("data"))
        returnIntent.putExtra("hasChanges", unsavedChanges)
        returnIntent.putExtra(Constants.ACC_CONFIG_KEY, mAccConfig)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putParcelable("mAccConfig", mAccConfig)
        outState?.putBoolean("unsavedChanges", unsavedChanges)

        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acc_config_editor)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = intent?.getStringExtra("title") ?: getString(R.string.acc_config_editor)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        unsavedChanges = savedInstanceState?.getBoolean("hasChanges", false) ?: false

        if(savedInstanceState?.containsKey("mAccConfig") == true) {
            this.mAccConfig = savedInstanceState.getParcelable("mAccConfig")!!
        } else if(intent.hasExtra(Constants.DATA_KEY)) {
            val bundle: Bundle = intent.getBundleExtra(Constants.DATA_KEY)
            mAccConfig = bundle.getParcelable(Constants.ACC_CONFIG_KEY)!!
        } else {
            try {
                this.mAccConfig = AccUtils.readConfig()
            } catch (ex: Exception) {
                ex.printStackTrace()
                showConfigReadError()
                this.mAccConfig = AccUtils.defaultConfig //if mAccConfig is null I use default mAccConfig values.
            }
        }

        initUi()
    }

    private fun showConfigReadError() {
        MaterialDialog(this).show {
            title(R.string.config_error_title)
            message(R.string.config_error_dialog)
            positiveButton(android.R.string.ok)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.acc_config_editor_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_save -> {
                returnResults()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if(unsavedChanges) {
            MaterialDialog(this)
                .show {
                    title(R.string.unsaved_changes)
                    message(R.string.unsaved_changes_message)
                    positiveButton(R.string.save) {
                        returnResults()
                    }
                    negativeButton(R.string.close_without_saving) {
                        finish()
                    }
                    neutralButton(android.R.string.cancel)
                }
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Function for On Boot ImageView OnClick.
     * Opens the dialog to edit the On Boot mAccConfig parameter.
     */
    fun editOnBootOnClick(view: View) {
        MaterialDialog(this@AccConfigEditorActivity).show {
                            title(R.string.edit_on_boot)
                message(R.string.edit_on_boot_dialog_message)
                input(prefill = this@AccConfigEditorActivity.mAccConfig.configOnBoot ?: "", allowEmpty = true, hintRes = R.string.edit_on_boot_dialog_hint) { _, text ->
                    this@AccConfigEditorActivity.mAccConfig.configOnBoot = text.toString()
                    this@AccConfigEditorActivity.tv_config_on_boot.text = if(text.isBlank()) getString(R.string.not_set) else text

                    unsavedChanges = true
                }
                positiveButton(R.string.save)
                negativeButton(android.R.string.cancel)
            }
    }

    fun editOnPluggedOnClick(v: View) {
        MaterialDialog(this@AccConfigEditorActivity).show {
            title(R.string.edit_on_plugged)
            message(R.string.edit_on_plugged_dialog_message)
            input(prefill = this@AccConfigEditorActivity.mAccConfig.configOnPlug ?: "", allowEmpty = true, hintRes = R.string.edit_on_boot_dialog_hint) { _, text ->
                this@AccConfigEditorActivity.mAccConfig.configOnPlug = text.toString()
                this@AccConfigEditorActivity.config_on_plugged_textview.text = if(text.isBlank()) getString(R.string.not_set) else text

                unsavedChanges = true
            }
            positiveButton(R.string.save)
            negativeButton(android.R.string.cancel)
        }
    }

    fun editChargingSwitchOnClick(v: View) {
        val automaticString = getString(R.string.automatic)
        val chargingSwitches = listOf(automaticString, *AccUtils.listChargingSwitches().toTypedArray())
        val initialSwitch = mAccConfig.configChargeSwitch
        var currentIndex = chargingSwitches.indexOf(initialSwitch ?: automaticString)

        MaterialDialog(this).show {
            title(R.string.edit_charging_switch)
            noAutoDismiss()

            setActionButtonEnabled(WhichButton.POSITIVE, currentIndex != -1)
            setActionButtonEnabled(WhichButton.NEUTRAL, currentIndex != -1)

            listItemsSingleChoice(items = chargingSwitches, initialSelection = currentIndex, waitForPositiveButton = false)  { _, index, text ->
                currentIndex = index

                setActionButtonEnabled(WhichButton.POSITIVE, index != -1)
                setActionButtonEnabled(WhichButton.NEUTRAL, index != -1)
            }

            positiveButton(R.string.save) {
                val index = currentIndex
                val switch = chargingSwitches[index]

                doAsync {
                    this@AccConfigEditorActivity.mAccConfig.configChargeSwitch = if(index == 0) null else switch
                    this@AccConfigEditorActivity.charging_switch_textview.text = this@AccConfigEditorActivity.mAccConfig.configChargeSwitch ?: getString(R.string.automatic)
                }

                this@AccConfigEditorActivity.unsavedChanges = true

                dismiss()
            }

            neutralButton(R.string.test_switch) {
                val switch = if(currentIndex == 0) null else chargingSwitches[currentIndex]

                Toast.makeText(this@AccConfigEditorActivity, R.string.wait, Toast.LENGTH_LONG).show()
                doAsync {
                    val description =
                        when(AccUtils.testChargingSwitch(switch)) {
                            0 -> R.string.charging_switch_works
                            1 -> R.string.charging_switch_does_not_work
                            2 -> R.string.plug_battery_to_test
                            else -> R.string.error_occurred
                        }

                    uiThread {
                        MaterialDialog(this@AccConfigEditorActivity).show {
                            title(R.string.test_switch)
                            message(description)
                            positiveButton(android.R.string.ok)
                        }
                    }
                }
            }

            negativeButton(android.R.string.cancel) {
                dismiss()
            }
        }
    }

    fun onInfoClick(v: View) {
        when(v.id) {
            R.id.capacity_control_info -> R.string.capacity_control_info
            R.id.voltage_control_info -> R.string.voltage_control_info
            R.id.temperature_control_info -> R.string.temperature_control_info
            R.id.exit_on_boot_info -> R.string.description_exit_on_boot
            R.id.cooldown_info -> R.string.cooldown_info
            R.id.on_plugged_info -> R.string.on_plugged_info
            else -> null
        }?.let {
            Tooltip.Builder(this)
                .anchor(v, 0, 0, false)
                .text(it)
                .arrow(true)
                .closePolicy(ClosePolicy.TOUCH_ANYWHERE_CONSUME)
                .showDuration(-1)
                .overlay(false)
                .maxWidth((resources.displayMetrics.widthPixels / 1.3).toInt())
                .create()
                .show(v, Tooltip.Gravity.LEFT, true)
        }

    }

    private fun initUi() {
        tv_config_on_boot.text = mAccConfig.configOnBoot?.let { if(it.isBlank()) getString(R.string.not_set) else it } ?: getString(R.string.not_set)
        exit_on_boot_switch.isChecked = mAccConfig.configOnBootExit
        exit_on_boot_switch.setOnCheckedChangeListener { _, isChecked ->
            mAccConfig.configOnBootExit = isChecked
            unsavedChanges = true
        }

        config_on_plugged_textview.text = mAccConfig.configOnPlug?.let { if(it.isBlank()) getString(R.string.not_set) else it } ?: getString(R.string.not_set)

        charging_switch_textview.text = mAccConfig.configChargeSwitch ?: getString(R.string.automatic)

        shutdown_capacity_picker.minValue = 0
        shutdown_capacity_picker.maxValue = 20
        shutdown_capacity_picker.value = mAccConfig.configCapacity.shutdown
        shutdown_capacity_picker.setOnValueChangedListener(this)

        resume_capacity_picker.minValue = mAccConfig.configCapacity.shutdown
        resume_capacity_picker.maxValue = mAccConfig.configCapacity.pause - 1
        resume_capacity_picker.value = mAccConfig.configCapacity.resume
        resume_capacity_picker.setOnValueChangedListener(this)

        pause_capacity_picker.minValue = mAccConfig.configCapacity.resume + 1
        pause_capacity_picker.maxValue = 100
        pause_capacity_picker.value = mAccConfig.configCapacity.pause
        pause_capacity_picker.setOnValueChangedListener(this)

        //temps
        if(mAccConfig.configTemperature.coolDownTemperature >= 90 && mAccConfig.configTemperature.pause >= 95) {
            temp_switch.isChecked = false
            cooldown_temp_picker.isEnabled = false
            pause_temp_picker.isEnabled = false
            pause_seconds_picker.isEnabled = false
        }
        temp_switch.setOnCheckedChangeListener(this)

        cooldown_temp_picker.minValue = 20
        cooldown_temp_picker.maxValue = 90
        cooldown_temp_picker.value = mAccConfig.configTemperature.coolDownTemperature
        cooldown_temp_picker.setOnValueChangedListener(this)

        pause_temp_picker.minValue = 20
        pause_temp_picker.maxValue = 95
        pause_temp_picker.value = mAccConfig.configTemperature.pause
        pause_temp_picker.setOnValueChangedListener(this)

        pause_seconds_picker.minValue = 10
        pause_seconds_picker.maxValue = 120
        pause_seconds_picker.value = mAccConfig.configTemperature.pause
        pause_seconds_picker.setOnValueChangedListener(this)

        //coolDown
        if(mAccConfig.configCoolDown.atPercent > 100) {
            cooldown_switch.isChecked = false
            cooldown_percentage_picker.isEnabled = false
            charge_ratio_picker.isEnabled = false
            pause_ratio_picker.isEnabled = false
        }
        cooldown_switch.setOnCheckedChangeListener(this)

        cooldown_percentage_picker.minValue = mAccConfig.configCapacity.shutdown
        cooldown_percentage_picker.maxValue = 101 //if someone wants to disable it should use the switch but I'm gonna leave it there
        cooldown_percentage_picker.value = mAccConfig.configCoolDown.atPercent
        cooldown_percentage_picker.setOnValueChangedListener(this)

        charge_ratio_picker.minValue = 1
        charge_ratio_picker.maxValue = 120 //no reason behind this value
        charge_ratio_picker.value = mAccConfig.configCoolDown.charge ?: 50
        charge_ratio_picker.setOnValueChangedListener(this)

        pause_ratio_picker.minValue = 1
        pause_ratio_picker.maxValue = 120 //no reason behind this value
        pause_ratio_picker.value = mAccConfig.configCoolDown.pause ?: 10
        pause_ratio_picker.setOnValueChangedListener(this)

        //voltage control
        voltage_control_file.text = mAccConfig.configVoltage.controlFile ?: "Not supported"
        voltage_max.text = mAccConfig.configVoltage.max?.let { "$it mV" } ?: getString(R.string.disabled)

        //Edit voltage dialog
        edit_voltage_limit.setOnClickListener {
            val dialog = MaterialDialog(this@AccConfigEditorActivity).show {
                customView(R.layout.voltage_control_editor_dialog)
                positiveButton(android.R.string.ok) { dialog ->
                    val view = dialog.getCustomView()
                    val voltageControl = view.findViewById<Spinner>(R.id.voltage_control_file)
                    val voltageMax = view.findViewById<EditText>(R.id.voltage_max)
                    val checkBox = dialog.findViewById<CheckBox>(R.id.enable_voltage_max)

                    val voltageMaxInt = voltageMax.text.toString().toIntOrNull()
                    if(checkBox.isChecked && voltageMaxInt != null) {
                        this@AccConfigEditorActivity.mAccConfig.configVoltage.max = voltageMaxInt
                        this@AccConfigEditorActivity.mAccConfig.configVoltage.controlFile = voltageControl.selectedItem as String

                        this@AccConfigEditorActivity.voltage_control_file.text = voltageControl.selectedItem as String
                        this@AccConfigEditorActivity.voltage_max.text = "$voltageMaxInt mV"
                    } else {
                        this@AccConfigEditorActivity.mAccConfig.configVoltage.max = null

                        this@AccConfigEditorActivity.voltage_max.text = getString(R.string.disabled)
                    }

                    unsavedChanges = true
                }
                negativeButton(android.R.string.cancel)
            }

            //initialize dialog custom view:
            val view = dialog.getCustomView()
            val voltageMax = view.findViewById<EditText>(R.id.voltage_max)
            val checkBox = dialog.findViewById<CheckBox>(R.id.enable_voltage_max)
            val voltageControl = view.findViewById<Spinner>(R.id.voltage_control_file)

            voltageMax.setText(mAccConfig.configVoltage.max?.toString() ?: "", TextView.BufferType.EDITABLE)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                voltageMax.isEnabled = isChecked

                val voltageMaxVal = voltageMax.text?.toString()?.toIntOrNull()
                val isValid = voltageMaxVal != null && voltageMaxVal >= 3920 && voltageMaxVal < 4200
                voltageMax.error = if (isValid) null else getString(R.string.invalid_voltage_max)
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid  && voltageControl.selectedItemPosition != -1)
            }
            checkBox.isChecked = mAccConfig.configVoltage.max != null
            voltageMax.isEnabled = checkBox.isChecked
            voltageMax.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val voltageMaxVal = s?.toString()?.toIntOrNull()
                    val isValid = voltageMaxVal != null && voltageMaxVal >= 3920 && voltageMaxVal < 4200
                    voltageMax.error = if(isValid) null else getString(R.string.invalid_voltage_max)
                    dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid  && voltageControl.selectedItemPosition != -1)
                }
            })

            val supportedVoltageControlFiles = ArrayList(AccUtils.listVoltageSupportedControlFiles())
            val currentVoltageFile = mAccConfig.configVoltage.controlFile?.let { currentVoltFile ->
                val currentVoltFileRegex = currentVoltFile.replace("/", """\/""").replace(".", """\.""").replace("?", ".").toRegex()
                val match = supportedVoltageControlFiles.find { currentVoltFileRegex.matches(it) }
                if(match == null) {
                    supportedVoltageControlFiles.add(currentVoltFile)
                    currentVoltFile
                } else {
                    match
                }
            }
            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, supportedVoltageControlFiles)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            voltageControl.adapter = adapter
            currentVoltageFile?.let {
                voltageControl.setSelection(supportedVoltageControlFiles.indexOf(currentVoltageFile))
            }
            if(voltageControl.selectedItemPosition == -1) {
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, false)
            }
            voltageControl.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    dialog.setActionButtonEnabled(WhichButton.POSITIVE, false)
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val voltageMaxVal = voltageMax.text?.toString()?.toIntOrNull()
                    val isValid = voltageMaxVal != null && voltageMaxVal >= 3920 && voltageMaxVal < 4200
                    dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid && position != -1)
                }
            }
        }
    }

    //Listener to enable/disable temp control and cool down
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if(buttonView == null) return
        when(buttonView.id) {
            R.id.temp_switch -> {
                cooldown_temp_picker.isEnabled = isChecked
                pause_temp_picker.isEnabled = isChecked
                pause_seconds_picker.isEnabled = isChecked

                if(isChecked) {
                    cooldown_temp_picker.value = 40
                    pause_temp_picker.value = 45

                    mAccConfig.configTemperature.coolDownTemperature = 40
                    mAccConfig.configTemperature.pause = 45
                    unsavedChanges = true
                } else {
                    mAccConfig.configTemperature.coolDownTemperature= 90
                    mAccConfig.configTemperature.pause = 95
                    unsavedChanges = true
                }
            }

            R.id.cooldown_switch -> {
                cooldown_percentage_picker.isEnabled = isChecked
                charge_ratio_picker.isEnabled = isChecked
                pause_ratio_picker.isEnabled = isChecked

                if(isChecked) {
                    cooldown_percentage_picker.value = 60
                    mAccConfig.configCoolDown.atPercent = 60
                    unsavedChanges = true
                } else {
                    cooldown_percentage_picker.value = 101
                    mAccConfig.configCoolDown.atPercent = 101
                    unsavedChanges = true
                }
            }

            else -> {}
        }
    }

    override fun onValueChange(picker: NumberPicker?, oldVal: Int, newVal: Int) {
        if(picker == null) return

        when(picker.id) {
            //capacity
            R.id.shutdown_capacity_picker -> {
                mAccConfig.configCapacity.shutdown = newVal

                resume_capacity_picker.minValue = mAccConfig.configCapacity.shutdown
                cooldown_percentage_picker.minValue = mAccConfig.configCapacity.shutdown
            }

            R.id.resume_capacity_picker -> {
                mAccConfig.configCapacity.resume = newVal

                pause_capacity_picker.minValue = mAccConfig.configCapacity.resume + 1
            }

            R.id.pause_capacity_picker -> {
                mAccConfig.configCapacity.pause = newVal

                resume_capacity_picker.maxValue = mAccConfig.configCapacity.pause - 1
                resume_capacity_picker.maxValue = mAccConfig.configCapacity.pause - 1
            }

            //temp
            R.id.cooldown_temp_picker ->
                mAccConfig.configTemperature.coolDownTemperature = newVal

            R.id.pause_temp_picker ->
                mAccConfig.configTemperature.pause = newVal

            R.id.pause_seconds_picker ->
                mAccConfig.configCoolDown.pause = newVal

            //coolDown
            R.id.cooldown_percentage_picker ->
                mAccConfig.configCoolDown.atPercent = newVal

            R.id.charge_ratio_picker -> {
                if(mAccConfig.configCoolDown.atPercent > 100) {
                    mAccConfig.configCoolDown.charge = newVal
                    mAccConfig.configCoolDown.pause = 10
                }
                mAccConfig.configCoolDown.charge = newVal
            }

            R.id.pause_ratio_picker -> {
                if(mAccConfig.configCoolDown.atPercent > 100) {
                    mAccConfig.configCoolDown.charge = 50
                    mAccConfig.configCoolDown.pause = newVal
                }
                mAccConfig.configCoolDown.pause = newVal
            }

            else -> {
                return //This allows to skip unsavedChanges = true
            }
        }

        unsavedChanges = true
    }
}
