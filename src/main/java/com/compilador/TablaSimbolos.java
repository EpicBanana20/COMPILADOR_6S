package com.compilador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TablaSimbolos {
    private final List<Simbolo> filas;
    private final Map<Integer, Map<String, Simbolo>> indicePorAmbito;

    public TablaSimbolos() {
        this.filas = new ArrayList<>();
        this.indicePorAmbito = new LinkedHashMap<>();
    }

    public void insertar(Simbolo simbolo) {
        filas.add(simbolo);
        indicePorAmbito
            .computeIfAbsent(simbolo.getAmb(), k -> new LinkedHashMap<>())
            .put(simbolo.getId(), simbolo);
    }

    public List<Simbolo> getFilas() {
        return filas;
    }

    public Map<Integer, Map<String, Simbolo>> getIndicePorAmbito() {
        return indicePorAmbito;
    }
}
