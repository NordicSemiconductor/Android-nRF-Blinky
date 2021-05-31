/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.blinky;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.appbar.MaterialToolbar;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import no.nordicsemi.android.ble.livedata.state.ConnectionState;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.viewmodels.BlinkyViewModel;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class BlinkyActivity extends AppCompatActivity {
	public static final String EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE";
	private static final int xIndex = 0;
	private static final int yIndex = 1;
	private static final int headIndex = 0;
	private static final int bpmValueIndex = 1;

	private BlinkyViewModel viewModel;

	@BindView(R.id.heart_rate_tv) TextView heart_rate_tv;
	@BindView(R.id.battery_level_tv) TextView batter_level_tv;
	@BindView(R.id.heart_rate_chart) LineChart chart;


	private ArrayList<int[]> hr_values_list = new ArrayList<>();
	private int data_cnt = 0;
	private int hrValue;
	private int batteryValue;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_blinky);
		ButterKnife.bind(this);

		final Intent intent = getIntent();
		final DiscoveredBluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
		final String deviceName = device.getName();
		final String deviceAddress = device.getAddress();

		final MaterialToolbar toolbar = findViewById(R.id.toolbar);
		toolbar.setTitle(deviceName != null ? deviceName : getString(R.string.unknown_device));
		toolbar.setSubtitle(deviceAddress);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Configure the view model.
		viewModel = new ViewModelProvider(this).get(BlinkyViewModel.class);
		viewModel.connect(device);

		// Set up views.
		final LinearLayout progressContainer = findViewById(R.id.progress_container);
		final TextView connectionState = findViewById(R.id.connection_state);
		final View content = findViewById(R.id.device_container);
		final View notSupported = findViewById(R.id.not_supported);

		//heart rate livedata observer
		viewModel.getHeartRate().observe(this, integer -> {
			hrValue = viewModel.getHeartRate().getValue();
			heart_rate_tv.setText(String.valueOf(hrValue));
			updateGraph();
		});

		//battery livedata observer
		viewModel.getBatteryLevel().observe(this, integer -> {
			batteryValue = viewModel.getBatteryLevel().getValue();
			batter_level_tv.setText(batteryValue + "%");
		});

		createChart();

		viewModel.getConnectionState().observe(this, state -> {
			switch (state.getState()) {
				case CONNECTING:
					progressContainer.setVisibility(View.VISIBLE);
					notSupported.setVisibility(View.GONE);
					connectionState.setText(R.string.state_connecting);
					break;
				case INITIALIZING:
					connectionState.setText(R.string.state_initializing);
					break;
				case READY:
					progressContainer.setVisibility(View.GONE);
					content.setVisibility(View.VISIBLE);
					break;
				case DISCONNECTED:
					if (state instanceof ConnectionState.Disconnected) {
						final ConnectionState.Disconnected stateWithReason = (ConnectionState.Disconnected) state;
						if (stateWithReason.isNotSupported()) {
							progressContainer.setVisibility(View.GONE);
							notSupported.setVisibility(View.VISIBLE);
						}
					}
					// fallthrough
				case DISCONNECTING:
					break;
			}
		});
	}

	private void createChart() {
		resetGraphData();

		LineData data = chart.getLineData();
		data.setValueTextColor(Color.CYAN);
		chart.setData(data);

		Description desc = chart.getDescription();
		desc.setText("");
		chart.setAutoScaleMinMaxEnabled(true);
		// touch screen functionality
		chart.setTouchEnabled(true);
		// move/zoom graph
		chart.setDragEnabled(true);
		chart.setScaleEnabled(true);
		chart.setDrawGridBackground(false);
		// pinch scale
		chart.setPinchZoom(true);
		// transparent background
		chart.setBackgroundColor(Color.TRANSPARENT);
		// padding
		chart.setExtraOffsets(10f, 10f, 10f, 10f);
		// add legend and edit, has to be done after setting data
		Legend hrm_key = chart.getLegend();
		hrm_key.setForm(Legend.LegendForm.LINE);
		hrm_key.setTextColor(Color.BLACK);



		// get x and y axis
		XAxis xax = chart.getXAxis();
		xax.setPosition(XAxis.XAxisPosition.BOTTOM);
		xax.setTextColor(Color.BLACK);
		xax.setDrawGridLines(false);
		xax.setAvoidFirstLastClipping(true);
		xax.setEnabled(true);

		YAxis lyax = chart.getAxisLeft();
		lyax.setTextColor(Color.BLACK);
		lyax.setDrawGridLines(true);
		lyax.setDrawTopYLabelEntry(true);
		lyax.setGridColor(Color.BLACK);
		lyax.setSpaceTop(0);
		lyax.setSpaceBottom(0);

		YAxis ryax = chart.getAxisRight();
		ryax.setDrawAxisLine(true);
		ryax.setDrawGridLines(false);
		ryax.setDrawLabels(false);

		chart.invalidate();
	}


	private LineDataSet formatSet(LineDataSet set) {
		set.setAxisDependency(YAxis.AxisDependency.LEFT);
		set.setColor(Color.CYAN);
		set.setLineWidth(2f);
		set.setFillAlpha(65);
		set.setValueTextColor(Color.BLACK);
		set.setValueTextSize(9f);
		set.setDrawValues(false);
		return set;

	}


	private void updateGraph() {
		resetGraphData();
		chart.notifyDataSetChanged();
		chart.invalidate();
	}

	//TODO - feature - options for graph view: live(1m), 10m(locked x axis), 1hr(locked x axis)
	private void resetGraphData() {
		//remove leading 0's
		if(!hr_values_list.isEmpty())
			if(hr_values_list.get(headIndex)[bpmValueIndex] == 0) {
				hr_values_list.remove(headIndex);
				data_cnt--;
			}
		hr_values_list.add(new int[]{data_cnt, hrValue});
		data_cnt++;
		List<Entry> entries = new ArrayList<Entry>();
		for (int[] data : hr_values_list) {
			entries.add(new Entry(data[xIndex], data[yIndex]));
		}
		LineDataSet init_data = new LineDataSet(entries, "BPM"); // add entries to dataset
		LineDataSet dataSet = formatSet(init_data);
		//dataSet.setColor(Color.parseColor("#0096FF"));
		LineData lineData = new LineData(dataSet);
		chart.setData(lineData);
	}

	@OnClick(R.id.action_clear_cache)
	public void onTryAgainClicked() {
		viewModel.reconnect();
	}

}
