package com.turistapp.jose.turistapp.Adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.google.gson.Gson;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.turistapp.jose.turistapp.Activities.PlaceInfoActivity;
import com.turistapp.jose.turistapp.Fragments.Places;
import com.turistapp.jose.turistapp.Model.Place;
import com.turistapp.jose.turistapp.R;

import java.util.ArrayList;
import java.util.List;

public class PlacesListAdapter extends ArrayAdapter<Place> {

    private final LayoutInflater mInflater;

    private final ViewBinderHelper viewBinderHelper = new ViewBinderHelper();

    private Context mcon;

    private ViewGroup mparent;

    ArrayList<Place> selectedroute = new ArrayList<>();

    private int originindex = 0;


    public PlacesListAdapter(Context context, List<Place> objects) {
        super(context, R.layout.places_item, objects);
        mcon = context;
        mInflater = LayoutInflater.from(context);
        viewBinderHelper.setOpenOnlyOne(true);


    }

    public ArrayList<Place> getSelected() {
        return this.selectedroute;
    }

    public Place getOrigin() {
        return getItem(originindex);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        mparent = parent;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.places_item, parent, false);

            holder = new ViewHolder();
            holder.placeimage = (CircularImageView) convertView.findViewById(R.id.locationimage_pilayout);
            holder.addbtn = (TextView) convertView.findViewById(R.id.addbtn_pilayout);
            holder.delbtn = (TextView) convertView.findViewById(R.id.delbtn_pilayout);
            holder.oribtn = (TextView) convertView.findViewById(R.id.oribtn_pilayout);
            holder.placename = (TextView) convertView.findViewById(R.id.locationname_pilayout);
            holder.selected = (ImageView) convertView.findViewById(R.id.selected_pilayout);
            holder.origin = (ImageView) convertView.findViewById(R.id.origin_pilayout);
            holder.swipeLayout = (SwipeRevealLayout) convertView.findViewById(R.id.swipe_pilayout);
            holder.mainLayout = (LinearLayout) convertView.findViewById(R.id.main_pilayout);


            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();

        }

        final Place item = getItem(position);

        if(position == originindex){
            holder.origin.setVisibility(View.VISIBLE);
        } else {
            holder.origin.setVisibility(View.INVISIBLE);
        }

        if(selectedroute.contains(item)){
            holder.selected.setVisibility(View.VISIBLE);
        } else {
            holder.selected.setVisibility(View.INVISIBLE);
        }
        if (item != null) {

            if(getPosition(item) == originindex ) {
                holder.origin.setVisibility(View.VISIBLE);
            }

            viewBinderHelper.bind(holder.swipeLayout, item.getName());

            holder.placename.setText(item.getName());

            RequestOptions reqOpt = RequestOptions
                    .fitCenterTransform()
                    .transform(new RoundedCorners(5))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(holder.placeimage.getWidth(),holder.placeimage.getHeight());

            Glide.with(getContext()).load(item.getImgurl()).thumbnail(/*sizeMultiplier=*/ 0.25f)
                    .apply(reqOpt).centerCrop().into(holder.placeimage);

            holder.addbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("ADAPTER: ","add button clicked");
                    holder.selected.setVisibility(View.VISIBLE);
                    holder.swipeLayout.close(true);

                    selectedroute.add(item);



                }
            });

            holder.delbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i("ADAPTER: ","remove button clicked");
                    holder.selected.setVisibility(View.INVISIBLE);
                    holder.swipeLayout.close(true);

                    selectedroute.remove(item);
                }
            });

            holder.oribtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Log.i("ADAPTER: ","origin button clicked");
                    parent.getChildAt(originindex).findViewById(R.id.origin_pilayout).setVisibility(View.INVISIBLE);
                    originindex = position;

                    holder.origin.setVisibility(View.VISIBLE);
                    holder.swipeLayout.close(true);


                }
            });

            holder.mainLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("ADAPTER: ", "main layout clicked");
                    Intent intent = new Intent(mcon, PlaceInfoActivity.class);
                    Gson gson = new Gson();
                    String lugarSerializada = gson.toJson(item);
                    intent.putExtra("place", lugarSerializada);
                    mcon.startActivity(intent);

                }
            });
        }

        return convertView;
    }

    /*
    * dejar de visualizar el indicador de origen
    * 0 dejar de ver
    * 1 ver nuevamente*/
    public void toggleOrigin(int i){
        if(i == 0){
            mparent.getChildAt(originindex).findViewById(R.id.origin_pilayout).setVisibility(View.INVISIBLE);

        } else if (i == 1){
            mparent.getChildAt(originindex).findViewById(R.id.origin_pilayout).setVisibility(View.VISIBLE);
        }
    }



    private class ViewHolder {
        View mainLayout;
        SwipeRevealLayout swipeLayout;
        CircularImageView placeimage;
        View addbtn;
        View delbtn;
        View oribtn;
        TextView placename;
        ImageView selected;
        ImageView origin;
    }
}
