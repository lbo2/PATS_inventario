package cl.pats.envy15.pats_inventario;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Created by Envy15 on 06/03/2015.
 */
public class AdaptadorBien extends BaseAdapter {

    private Context mContext;
    private Bien[] mBienes;

    public AdaptadorBien(Context context, Bien[] bienes){
        mContext = context;
        mBienes = bienes;
    }

    @Override
    public int getCount() {
        return mBienes.length;
    }

    @Override
    public Object getItem(int position) {
        return mBienes[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView==null){
            //nuevo
            convertView = LayoutInflater.from(mContext).inflate(R.layout.bien_item, null);
            holder = new ViewHolder();
            holder.txt_articulo = (TextView) convertView.findViewById(R.id.txt_articulo);
            holder.txt_micropunto = (TextView) convertView.findViewById(R.id.txt_micropunto);
            holder.txt_sello = (TextView) convertView.findViewById(R.id.txt_sello);
            holder.txt_sucursal = (TextView) convertView.findViewById(R.id.txt_sucursal);

            convertView.setTag(holder);
        }
        else{
            holder = (ViewHolder) convertView.getTag();
        }

        Bien bien = mBienes[position];
        holder.txt_articulo.setText(bien.getArticulo());
        holder.txt_sello.setText(bien.getSelloPats());
        holder.txt_micropunto.setText(bien.getMicropunto());
        holder.txt_sucursal.setText(bien.getSucursal());

        return convertView;
    }

    private static class ViewHolder{
        TextView txt_articulo;
        TextView txt_micropunto;
        TextView txt_sello;
        TextView txt_sucursal;
    }
}
