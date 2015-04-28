package cl.pats.envy15.pats_inventario;

/**
 * Created by Envy15 on 06/03/2015.
 */
public class Bien {
    private String mIdBien;
    private String mArticulo;
    private String mSelloPats;
    private String mMicropunto;
    private String mSucursal;

    public String getIdBien() {
        return mIdBien;
    }

    public void setIdBien(String idBien) {
        mIdBien = idBien;
    }

    public String getArticulo() {
        return mArticulo;
    }

    public void setArticulo(String articulo) {
        mArticulo = articulo;
    }

    public String getSelloPats() {
        return mSelloPats;
    }

    public void setSelloPats(String selloPats) {
        mSelloPats = selloPats;
    }

    public String getMicropunto() {
        return mMicropunto;
    }

    public void setMicropunto(String micropunto) {
        mMicropunto = micropunto;
    }

    public String getSucursal() { return mSucursal; }

    public void setSucursal(String sucursal) { mSucursal = sucursal;    }
}
