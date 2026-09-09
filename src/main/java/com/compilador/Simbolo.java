package com.compilador;

public class Simbolo {
    private String id;
    private String tipo;
    private String clase;
    private int amb;
    private String tarr;
    private String dimArr;
    private String noPar;
    private String tParr;

    public Simbolo(String id, String tipo, String clase, int amb,
                    String tarr, String dimArr, String noPar, String tParr) {
        this.id = id;
        this.tipo = tipo;
        this.clase = clase;
        this.amb = amb;
        this.tarr = tarr;
        this.dimArr = dimArr;
        this.noPar = noPar;
        this.tParr = tParr;
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getClase() {
        return clase;
    }

    public void setClase(String clase) {
        this.clase = clase;
    }

    public int getAmb() {
        return amb;
    }

    public String getTarr() {
        return tarr;
    }

    public void setTarr(String tarr) {
        this.tarr = tarr;
    }

    public String getDimArr() {
        return dimArr;
    }

    public void setDimArr(String dimArr) {
        this.dimArr = dimArr;
    }

    public String getNoPar() {
        return noPar;
    }

    public void setNoPar(String noPar) {
        this.noPar = noPar;
    }

    public String getTParr() {
        return tParr;
    }
}
