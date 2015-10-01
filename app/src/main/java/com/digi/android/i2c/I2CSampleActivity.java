package com.digi.android.i2c;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.i2c.I2C;
import android.i2c.NoSuchInterfaceException;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This application demonstrates the usage of the I2C API by accessing and
 * controlling an I2C slave EEPROM memory. An external I2C EEPROM memory is
 * required to try this sample.
 *
 * <p>Application allows user to configure I2C interface to use as well as
 * device slave address.</p>
 *
 * <p>Once I2C interface is opened, application has the possibility to read
 * data from the EEPROM memory, write data or erase data. Read results are
 * displayed in an hexadecimal table at the right side of the application.</p>
 */
public class I2CSampleActivity extends Activity implements OnClickListener {

	// Constants
	private final static int NUM_BYTES = 256;
	private final static int PAGE_SIZE = 32;
	private final static int HEX_BUF_SIZE = 8;
	private final static int BASE_ADDRESS = 0x0000;
	
	private final static String SLAVE_ADDRESS_PATTERN = "[0-9a-fA-F]{1,2}";
	
	// UI Elements
	private Spinner interfaceSelector;
	
	private EditText slaveAddressText;
	
	private Button openInterfaceButton;
	private Button closeInterfaceButton;
	private Button readDataButton;
	private Button writeDataButton;
	private Button eraseDataButton;
	
	private ListView hexDataList;
	
	// Variables
	private ArrayList<HexRow> hexRows = new ArrayList<HexRow>();
	
	private HexRowsAdapter hexRowsAdapter;
	
	private I2C i2cInterface;
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        initializeUIElements();
        fillI2CInterfaces();
        initializeDataList();
        validatePage();
    }
    
    /**
     * Initializes all UI elements and sets required listeners.
     */
    private void initializeUIElements() {
    	// Instance elements from layout
    	interfaceSelector = (Spinner)findViewById(R.id.interface_selector);
    	slaveAddressText = (EditText)findViewById(R.id.slave_address);
    	openInterfaceButton = (Button)findViewById(R.id.open_interface);
    	closeInterfaceButton = (Button)findViewById(R.id.close_interface);
    	readDataButton = (Button)findViewById(R.id.read_button);
    	writeDataButton = (Button)findViewById(R.id.write_button);
    	eraseDataButton = (Button)findViewById(R.id.erase_button);
    	hexDataList = (ListView)findViewById(R.id.hex_data_list);
    	// Set touch listeners
    	interfaceSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
    		public void onItemSelected(AdapterView<?> arg0, View arg1,
    				int arg2, long arg3) {
    			validatePage();
    		}
    		
    		public void onNothingSelected(AdapterView<?> arg0) {
    			validatePage();
    		}
		});
    	openInterfaceButton.setOnClickListener(this);
    	closeInterfaceButton.setOnClickListener(this);
    	readDataButton.setOnClickListener(this);
    	writeDataButton.setOnClickListener(this);
    	eraseDataButton.setOnClickListener(this);
    	slaveAddressText.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {
				validatePage();
			}
		});
    	openInterfaceButton.requestFocus();
    }
    
    /**
     * Lists and fills available I2C interfaces.
     */
    private void fillI2CInterfaces() {
    	int[] interfaces = I2C.listInterfaces();
    	String[] interfacesArray = new String[interfaces.length];
    	for (int i = 0; i < interfaces.length; i++)
    		interfacesArray[i] = String.valueOf(interfaces[i]);
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, interfacesArray);
    	adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
    	interfaceSelector.setAdapter(adapter);
    	if (interfaceSelector.getItemAtPosition(0) != null)
    		interfaceSelector.setSelection(0);
    }
    
    /**
     * Initializes Hex data dump with null values (nothing has been read yet).
     */
    private void initializeDataList() {
    	for (int i = 0; i < NUM_BYTES/8; i++) {
    		HexRow row = new HexRow(i, null);
    		hexRows.add(row);
    	}
    	hexRowsAdapter = new HexRowsAdapter(this, R.layout.hex_row, hexRows);
        hexDataList.setAdapter(hexRowsAdapter);
    }
    
    /**
     * Attempts to open configured I2C interface.
     */
    private void openInterface() {
    	i2cInterface = new I2C(Integer.valueOf(interfaceSelector.getSelectedItem().toString()), 
    			Integer.parseInt(slaveAddressText.getText().toString(), 16));
    	try {
			i2cInterface.open();
			Toast.makeText(this, "I2C Interface " + interfaceSelector.getSelectedItem().toString() +
					" opened with slave address 0x" + slaveAddressText.getText(), Toast.LENGTH_SHORT).show();
		} catch (NoSuchInterfaceException e) {
			Toast.makeText(this, "Error opening interface: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} catch (IOException e) {
			Toast.makeText(this, "Error opening interface: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
    	updateButtons();
    }
    
    /**
     * Attempts to close current I2C interface.
     */
    private void closeInterface() {
    	if (i2cInterface == null)
    		return;
    	if (i2cInterface.isInterfaceOpen())
    		i2cInterface.close();
    	Toast.makeText(this, "I2C Interface " + interfaceSelector.getSelectedItem().toString() +
				" closed.", Toast.LENGTH_SHORT).show();
    	updateButtons();
    }
    
    /**
     * Reads data from active I2C interface and fills hex data dump.
     */
    private void readData() {
    	// Check active I2C interface integrity.
    	if (i2cInterface == null || !i2cInterface.isInterfaceOpen()) {
    		updateButtons();
    		return;
    	}
    	try {
    		// Set address to read from
    		i2cInterface.write(new byte[]{intToByteArray(BASE_ADDRESS)[2], intToByteArray(BASE_ADDRESS)[3]});
    		// Perform data read
			byte[] data = i2cInterface.read(NUM_BYTES);
			// Draw data
			hexRows.clear();
			int id = 0;
			ByteArrayInputStream stream = new ByteArrayInputStream(data);
			while (stream.available() > 0) {
				byte[] buffer = new byte[HEX_BUF_SIZE];
				stream.read(buffer, 0, HEX_BUF_SIZE);
				HexRow hexRow = new HexRow(id, buffer);
				hexRows.add(hexRow);
				id++;
			}
			hexRowsAdapter.notifyDataSetChanged();
		} catch (IOException e) {
			Toast.makeText(this, "Error reading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			return;
		}
		Toast.makeText(this, "Memory was read successfully.", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Writes data to the active I2C interface.
     */
    private void writeData() {
    	// Check active I2C interface integrity.
    	if (i2cInterface == null || !i2cInterface.isInterfaceOpen()) {
    		updateButtons();
    		return;
    	}
    	// Prepare data to write setting address in first 2 bytes
    	byte[] data = new byte[NUM_BYTES];
    	for (int i = 0; i < NUM_BYTES; i++)
    		data[i] = (byte)i;
    	if (writeEEPROMData(BASE_ADDRESS, data, PAGE_SIZE))
    		Toast.makeText(this, "Memory was written successfully.", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Special EEPROM algorithm designed to write data in these kind of memories.
     * 
     * @param address Starting address to write data to.
     * @param data Data to be written.
     * @return True if success, false otherwise.
     */
    private boolean writeEEPROMData(int address, byte[] data, int pageSize) {
    	try {
    		int bytesWritten = 0;
    		ByteArrayInputStream stream = new ByteArrayInputStream(data);
    		while (bytesWritten < data.length) {
    			Thread.sleep(10);
    			if ((address & (pageSize - 1)) != 0) {
    				byte[] buffer = new byte[3];
    				buffer[0] = intToByteArray(address)[2];
    				buffer[1] = intToByteArray(address)[3];
    				stream.read(buffer, 2, 1);
    				i2cInterface.write(buffer);
    				address = address + 1;
    				bytesWritten = bytesWritten + 1;
    			} else {
    				byte[] buffer = new byte[pageSize + 2];
    				buffer[0] = intToByteArray(address)[2];
    				buffer[1] = intToByteArray(address)[3];
    				stream.read(buffer, 2, pageSize);
    				i2cInterface.write(buffer);
    				address = address + pageSize;
    				bytesWritten = bytesWritten + pageSize;
    			}
    		}
		} catch (IOException e) {
			Toast.makeText(this, "Error writing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			Toast.makeText(this, "Error writing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			return false;
		}
		return true;
    }
    
    /**
     * Writes FF data to the active I2C interface.
     */
    private void eraseData() {
    	// Check active I2C interface integrity.
    	if (i2cInterface == null || !i2cInterface.isInterfaceOpen()) {
    		updateButtons();
    		return;
    	}
    	// Prepare data to write setting address in first 2 bytes
    	byte[] data = new byte[NUM_BYTES];
    	for (int i = 0; i < NUM_BYTES; i++)
    		data[i] = (byte)0xFF;
    	if (writeEEPROMData(BASE_ADDRESS, data, PAGE_SIZE))
    		Toast.makeText(this, "Memory was erased successfully.", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Validates current page I2C values enabling and disabling buttons as required.
     */
    private void validatePage() {
    	if (interfaceSelector.getSelectedItem() == null) {
    		Toast.makeText(this, "Invalid I2C interface", Toast.LENGTH_SHORT).show();
    		slaveAddressText.setEnabled(false);
    		openInterfaceButton.setEnabled(false);
    		return;
    	}
    	if (slaveAddressText.isEnabled() && !Pattern.matches(SLAVE_ADDRESS_PATTERN, slaveAddressText.getText())) {
    		Toast.makeText(this, "Invalid I2C slaveAddress", Toast.LENGTH_SHORT).show();
    		openInterfaceButton.setEnabled(false);
    		return;
    	}
    	slaveAddressText.setEnabled(true);
    	openInterfaceButton.setEnabled(true);
    }
    
    /**
     * Updates layout buttons depending on current I2C interface status.
     */
    private void updateButtons() {
    	openInterfaceButton.setEnabled(i2cInterface!= null && !i2cInterface.isInterfaceOpen());
    	closeInterfaceButton.setEnabled(i2cInterface!= null && i2cInterface.isInterfaceOpen());
    	readDataButton.setEnabled(i2cInterface!= null && i2cInterface.isInterfaceOpen());
    	writeDataButton.setEnabled(i2cInterface!= null && i2cInterface.isInterfaceOpen());
    	eraseDataButton.setEnabled(i2cInterface!= null && i2cInterface.isInterfaceOpen());
    	interfaceSelector.setEnabled(i2cInterface!= null && !i2cInterface.isInterfaceOpen());
    	slaveAddressText.setEnabled(i2cInterface!= null && !i2cInterface.isInterfaceOpen());
    }
    
    /**
     * Retrieves hexadecimal string representation of the given byte array.
     * 
     * @param data Byte array to get Hex String representation from.
     * @return Hex String of the given byte array.
     * @throws Exception
     */
    public static String getHexString(byte[] data) throws Exception {
    	String result = "";
    	for (int i = 0; i < data.length; i++)
    		result += Integer.toString((data[i] & 0xff) + 0x100, 16).substring(1);
    	return result.toUpperCase();
    }
    
    /**
     * Converts the given integer into a byte array.
     * 
     * @param value Integer to convert to byte array.
     * @return The integer as byte array;
     */
    public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }
    
	/*
	 * (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v) {
		// Attend only to touch up events
		switch (v.getId()) {
		case R.id.open_interface:
			openInterface();
			break;
		case R.id.close_interface:
			closeInterface();
			break;
		case R.id.read_button:
			readData();
			break;
		case R.id.write_button:
			writeData();
			break;
		case R.id.erase_button:
			eraseData();
			break;
		}
	}
    
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onWindowFocusChanged(boolean)
     */
    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onWindowFocusChanged(hasFocus);
    	// Ensure initial window focus is set to the open interface button
    	if (hasFocus) {
    		openInterfaceButton.requestFocus();
    		openInterfaceButton.requestFocusFromTouch();
    	}
    }
    
    /**
	 * Populates a ListView with the data contained in the given ArrayList 
	 */
	class HexRowsAdapter extends ArrayAdapter<HexRow> {    
		private ArrayList<HexRow> hexRows;

		HexRowsAdapter(Context context, int textViewResourceId, ArrayList<HexRow> items) {         
			super(context, textViewResourceId, items);
			hexRows = items;
		}

		/*
		 * (non-Javadoc)
		 * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
		 */
		public View getView(int position, View convertView, ViewGroup parent){
			View row = null;
			if (convertView != null)
				row = convertView;
			LayoutInflater inflater = getLayoutInflater();
			HexRow item = hexRows.get(position);
			row = inflater.inflate(R.layout.hex_row, parent, false);
			TextView rowId = (TextView)row.findViewById(R.id.row_id);
			TextView hexData = (TextView)row.findViewById(R.id.hex_data);
			TextView asciiData = (TextView)row.findViewById(R.id.ascii_data);
			rowId.setText(item.getIdString());
			hexData.setText(item.getHexDataString());
			asciiData.setText(item.getAsciiString());
			return(row);
		}
	}
	
	public class HexRow {
		
		private int id;
		
		private byte[] hexData;
		
		public HexRow(int id, byte[] hexData) {
			this.id = id;
			this.hexData = hexData;
		}
		
		public String getIdString() {
			String value = "" + (id * 8);
			if (value.length() < 3) {
				int diff = 3 -value.length();
				for (int i = 0; i < diff; i++)
					value = "0" + value;
			}
			return value + " | ";
		}
		
		public String getHexDataString() {
			String value = "";
			if (hexData == null)
				return "";
			for (int i = 0; i < hexData.length; i++) {
				try {
					value = value + getHexString(new byte[]{hexData[i]}) + " ";
				} catch (Exception e) {}
			}
			return value + "|";
		}
		
		public String getAsciiString() {
			if (hexData == null)
				return "";
			String value = new String(hexData);
			return value;
		}
	}
}
