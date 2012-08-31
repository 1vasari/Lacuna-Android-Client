package com.lacunaexpanse.LacunaAndroidClient;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class PlanetView extends Activity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planet_view);
        
        final long spinnerReset = System.currentTimeMillis();
        
        ProgressDialog loadingDialog = Library.loadingDialog(PlanetView.this, "Loading...");
        loadingDialog.show();
        
        String sessionId = null;
        String serverResponseFromPreviousActivity = null;
        String selectedServer = null;
        Bundle extras = getIntent().getExtras(); 
        if (extras != null) {
        	sessionId = extras.getString("sessionId");
        	serverResponseFromPreviousActivity = extras.getString("serverResponse");
        	selectedServer = extras.getString("selectedServer");
        }

		// Get home_planet_id
		String homePlanetId = null;
		try {
			JSONObject jObject = new JSONObject(serverResponseFromPreviousActivity);
			JSONObject result = jObject.getJSONObject("result");
			JSONObject status = result.getJSONObject("status");
			JSONObject empire = status.getJSONObject("empire");

			homePlanetId = empire.getString("home_planet_id");
		}
		catch (JSONException e) {
			loadingDialog.dismiss();
			Library.handleError(PlanetView.this, serverResponseFromPreviousActivity, loadingDialog);
		}

		String serverResponse = refreshResources(sessionId,homePlanetId,selectedServer,loadingDialog);


		// Parse the JSON for getting the list of planets
		Object[] planetIds = null;
		try {
			JSONObject jObject = new JSONObject(serverResponseFromPreviousActivity);
			JSONObject result = jObject.getJSONObject("result");
			JSONObject status = result.getJSONObject("status");
			JSONObject empire = status.getJSONObject("empire");
			JSONObject planets = empire.getJSONObject("planets");

			ArrayList<String> arrayOne = new ArrayList<String>();
			ArrayList<String> arrayTwo = new ArrayList<String>();
	        Iterator<?> iter = planets.keys();

	        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
	        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        Spinner selectPlanetSpinner = (Spinner) findViewById(R.id.selectPlanet);

	        while(iter.hasNext()){
	            String key = (String)iter.next();
	            String value = planets.getString(key);

	            arrayTwo.add(key);
	            arrayOne.add(value);
	        }

	        Object[] planetNames = arrayOne.toArray();
			/*Object[]*/ planetIds = arrayTwo.toArray();

	        for (int i = 0; i < planetNames.length; i++) {
	        	adapter.add(planetNames[i].toString());
	        }

	        selectPlanetSpinner.setAdapter(adapter);

		}
		catch (JSONException e) {
			loadingDialog.dismiss();
			Library.handleError(PlanetView.this, serverResponse, loadingDialog);
		}

		// At long last, the loading has finished! :D
		loadingDialog.dismiss();

		final Object[] planetIdsOne = planetIds;
		final String sessionIdOne = sessionId;
		final String selectedServerOne = selectedServer;
		final ProgressDialog loadingDialogOne = loadingDialog;

		Spinner spinner = (Spinner) findViewById(R.id.selectPlanet);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent,View view,int pos,long id) {
				if (System.currentTimeMillis() - spinnerReset < 2500) {
					//Nothing!
				}
				else if (System.currentTimeMillis() - spinnerReset > 2500) {
					String selectedBodyId = planetIdsOne[pos].toString();

					refreshResources(sessionIdOne,selectedBodyId,selectedServerOne,loadingDialogOne);
				}
			}
			public void onNothingSelected(AdapterView<?> parent) {
				// Nothing!
			}
		});

		// Handle clicking of the "Logout" button.
		final String sessionIdTwo = sessionId;
		final String selectedServerTwo = selectedServer;

		Button logoutButton = (Button) findViewById(R.id.logoutButton);
		logoutButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ProgressDialog loadingDialog = Library.loadingDialog(PlanetView.this, "Loading...");
				loadingDialog.show();

				String[] paramsBuilder = {sessionIdTwo};
				String params = Library.parseParams(paramsBuilder);
				String serverUrl = Library.assembleGetUrl(selectedServerTwo, "empire", "logout", params);

				String serverResponseOne = Library.sendServerRequest(serverUrl);

				//Parse JSON
				int result = 0;
				try {
					JSONObject jObject = new JSONObject(serverResponseOne);
					result = jObject.getInt("result");
				}
				catch (JSONException e) {
					loadingDialog.dismiss();
					Library.handleError(PlanetView.this, serverResponseOne, loadingDialog);
				}

				if (result == 1) {
					loadingDialog.dismiss();
					Intent intent = new Intent(PlanetView.this,Login.class);
					PlanetView.this.startActivity(intent);
					finish();
				}
				else {
					loadingDialog.dismiss();
					Toast.makeText(PlanetView.this, "Something stupido has happened while the logout request was being made...", Toast.LENGTH_LONG).show();
				}
			}
		});
    }
    
    public String refreshResources(String sessionId,String homePlanetId,String selectedServer,ProgressDialog loadingdialog) {
    	ProgressDialog loadingDialog = Library.loadingDialog(PlanetView.this, "Loading...");
    	loadingDialog.show();
    	
    	String[] paramsBuilder = {sessionId,homePlanetId};
		String params = Library.parseParams(paramsBuilder);
		String serverUrl = Library.assembleGetUrl(selectedServer, "body", "get_status", params);

		String serverResponse = Library.sendServerRequest(serverUrl);

		TextView planetNameOutput = (TextView) findViewById(R.id.planetName);
		TextView foodInformationOutput = (TextView) findViewById(R.id.foodProductionInfo);
		TextView oreInformationOutput = (TextView) findViewById(R.id.oreProductionInfo);
		TextView waterInformationOutput = (TextView) findViewById(R.id.waterProductionInfo);
		TextView energyInformationOutput = (TextView) findViewById(R.id.energyProductionInfo);
		TextView wasteInformationOutput = (TextView) findViewById(R.id.wasteProductionInfo);

		// Parse JSON from server getting production/resource information
		long foodProduction   = 0;
		long foodStorage      = 0;
		long foodStored       = 0;

		long oreProduction    = 0;
		long oreStorage       = 0;
		long oreStored        = 0;

		long waterProduction  = 0;
		long waterStorage     = 0;
		long waterStored      = 0;

		long energyProduction = 0;
		long energyStorage    = 0;
		long energyStored     = 0;

		long wasteProduction  = 0;
		long wasteStorage     = 0;
		long wasteStored      = 0;

		String planetName       = null;
		try {
			JSONObject jObject = new JSONObject(serverResponse);
			JSONObject result = jObject.getJSONObject("result");
			JSONObject body = result.getJSONObject("body");

			// Get food information
			foodProduction = body.getLong("food_hour");
			foodStorage = body.getLong("food_capacity");
			foodStored = body.getLong("food_stored");

			// Get ore information
			oreProduction = body.getLong("ore_hour");
			oreStorage = body.getLong("ore_capacity");
			oreStored = body.getLong("ore_stored");

			// Get water information
			waterProduction = body.getLong("water_hour");
			waterStorage = body.getLong("water_capacity");
			waterStored = body.getLong("water_stored");

			// Get energy information
			energyProduction = body.getLong("energy_hour");
			energyStorage = body.getLong("energy_capacity");
			energyStored = body.getLong("energy_stored");

			// Get waste information
			wasteProduction = body.getLong("waste_hour");
			wasteStorage = body.getLong("waste_capacity");
			wasteStored = body.getLong("waste_stored");

			planetName = body.getString("name");
		}
		catch (JSONException e) {
			loadingDialog.dismiss();
			Library.handleError(PlanetView.this, serverResponse, loadingDialog);
		}
		
		// Organize the big numbers the server has returned
		String miniFoodStored = Library.formatBigNumbers(foodStored);
		String miniFoodStorage = Library.formatBigNumbers(foodStorage);
		String miniFoodProduction = Library.formatBigNumbers(foodProduction);
		
		String miniOreStored = Library.formatBigNumbers(oreStored);
		String miniOreStorage = Library.formatBigNumbers(oreStorage);
		String miniOreProduction = Library.formatBigNumbers(oreProduction);
		
		String miniWaterStored = Library.formatBigNumbers(waterStored);
		String miniWaterStorage = Library.formatBigNumbers(waterStorage);
		String miniWaterProduction = Library.formatBigNumbers(waterProduction);
		
		String miniEnergyStored = Library.formatBigNumbers(energyStored);
		String miniEnergyStorage = Library.formatBigNumbers(energyStorage);
		String miniEnergyProduction = Library.formatBigNumbers(energyProduction);
		
		String miniWasteStored = Library.formatBigNumbers(wasteStored);
		String miniWasteStorage = Library.formatBigNumbers(wasteStorage);
		String miniWasteProduction = Library.formatBigNumbers(wasteProduction);
		
		planetNameOutput.setText("Planet: " + planetName);
		foodInformationOutput.setText("Food: " + miniFoodStored + "/" + miniFoodStorage + " @ " + miniFoodProduction + "/hr");
		oreInformationOutput.setText("Ore: " + miniOreStored + "/" + miniOreStorage + " @ " + miniOreProduction + "/hr");
		waterInformationOutput.setText("Water: " + miniWaterStored + "/" + miniWaterStorage + " @ " + miniWaterProduction + "/hr");
		energyInformationOutput.setText("Energy: " + miniEnergyStored + "/" + miniEnergyStorage + " @ " + miniEnergyProduction + "/hr");
		wasteInformationOutput.setText("Waste: " + miniWasteStored + "/" + miniWasteStorage + " @ " + miniWasteProduction + "/hr");

		loadingDialog.dismiss();

		//For when the planets spinner needs updating
		return serverResponse;
    }
}