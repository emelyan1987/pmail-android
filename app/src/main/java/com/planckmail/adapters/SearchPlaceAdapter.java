package com.planckmail.adapters;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.planckmail.R;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.dao.AutoCompletePlace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by Terry on 12/7/2015.
 */
public class SearchPlaceAdapter extends RecycleViewArrayAdapter<AutoCompletePlace, SearchPlaceAdapter.ViewHolderSearchPlace> implements Filterable {
    private final GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private OnGetLocationResult mListener;

    public SearchPlaceAdapter(Context context, GoogleApiClient googleApiClient, List<AutoCompletePlace> list) {
        super(list);
        mContext = context;
        mGoogleApiClient = googleApiClient;
    }

    @Override
    public Filter getFilter() {
        return new AddressFilter();
    }

    @Override
    public ViewHolderSearchPlace onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.elem_search_place, parent, false);
        return new ViewHolderSearchPlace(view);
    }

    @Override
    public void onBindViewHolder(ViewHolderSearchPlace holder, AutoCompletePlace item) {
        holder.cardView.setTag(R.string.tag_location, item.getName());
        holder.city.setText(item.getCity());
        holder.address.setText(item.getName());
    }

    public boolean getData() {
        return isEmpty();
    }

    public void setListener(OnGetLocationResult listener) {
        mListener = listener;
    }


    private class AddressFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (mGoogleApiClient == null || !mGoogleApiClient.isConnected())
                return null;

            clear();
            AsyncFindLocation task = new AsyncFindLocation();
            task.execute(constraint.toString());

            return null;
        }

        /**
         * Notify about filtered list to ui
         *
         * @param constraint text
         * @param results    filtered result
         */
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }

        private void displayPredictiveResults(String query) {

            //Filter: https://developers.google.com/places/supported_types#table3
            List<Integer> filterTypes = new ArrayList<>();
            filterTypes.add(Place.TYPE_COUNTRY);
            //    filterTypes.add(Place.TYPE_ESTABLISHMENT);
            filterTypes.add(Place.TYPE_SUBLOCALITY);

            Places.GeoDataApi.getAutocompletePredictions(mGoogleApiClient, query, null, AutocompleteFilter.create(filterTypes))
                    .setResultCallback(
                            new ResultCallback<AutocompletePredictionBuffer>() {
                                @Override
                                public void onResult(AutocompletePredictionBuffer buffer) {

                                    if (buffer == null)
                                        return;

                                    if (buffer.getStatus().isSuccess()) {
                                        for (AutocompletePrediction prediction : buffer) {
                                            //Add as a new item to avoid IllegalArgumentsException when buffer is released
                                            findPlaceById(prediction.getPlaceId());
                                        }
                                    }
                                    //Prevent memory leak by releasing buffer
                                    buffer.release();
                                }
                            }, 60, TimeUnit.SECONDS);
        }

        private void findPlaceById(String id) {
            if (TextUtils.isEmpty(id) || mGoogleApiClient == null || !mGoogleApiClient.isConnected())
                return;

            Places.GeoDataApi.getPlaceById(mGoogleApiClient, id).setResultCallback(new ResultCallback<PlaceBuffer>() {
                @Override
                public void onResult(PlaceBuffer places) {
                    if (places.getStatus().isSuccess()) {
                        for (Place place : places) {
                            LatLng latLong = place.getLatLng();
                            Address address = getAddress(mContext, latLong.latitude, latLong.longitude);
                            if (address != null)
                                add(new AutoCompletePlace(address.getLocality(), place.getAddress().toString()));
                        }
                    }
                    //Release the PlaceBuffer to prevent a memory leak
                    places.release();
                    if (mListener != null)
                        mListener.getResultLocation(isEmpty());
                }
            });
        }

        public Address getAddress(Context context, double latitude, double longitude) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    return addresses.get(0);
                }
                return null;
            } catch (IOException ignored) {
                Log.e(PlanckMailApplication.TAG, ignored.getMessage());
            }
            return null;
        }

        private class AsyncFindLocation extends AsyncTask<String, Void, Void> {

            @Override
            protected Void doInBackground(String... params) {
                String searchedText = params[0];
                displayPredictiveResults(searchedText);
                return null;
            }
        }

    }

    public class ViewHolderSearchPlace extends RecyclerView.ViewHolder {
        public TextView city;
        public TextView address;
        public CardView cardView;

        public ViewHolderSearchPlace(View itemView) {
            super(itemView);
            city = (TextView) itemView.findViewById(R.id.tvCity);
            address = (TextView) itemView.findViewById(R.id.tvAddress);
            cardView = (CardView) itemView.findViewById(R.id.carSearchedLocation);
        }
    }

    public interface OnGetLocationResult {
        void getResultLocation(boolean isDataEmpty);
    }
}
