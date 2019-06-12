package br.ufsm.csi.seguranca.pila.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

public class Usuario implements Serializable {

    private static final long serialVersionUID = 2L;

    private String id;
    private PublicKey chavePublica;
    private InetAddress endereco;
    private Set<PilaCoin> meusPilas = new HashSet<>();

    public Set<PilaCoin> getMeusPilas() {
        return meusPilas;
    }

    public void setMeusPilas(Set<PilaCoin> meusPilas) {
        this.meusPilas = meusPilas;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PublicKey getChavePublica() {
        return chavePublica;
    }

    public void setChavePublica(PublicKey chavePublica) {
        this.chavePublica = chavePublica;
    }

    public InetAddress getEndereco() {
        return endereco;
    }

    public void setEndereco(InetAddress endereco) {
        this.endereco = endereco;
    }
}
