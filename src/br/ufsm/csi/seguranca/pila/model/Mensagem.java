package br.ufsm.csi.seguranca.pila.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.security.PublicKey;


/**
 * Created by cpol on 17/04/2018.
 */
public class Mensagem implements Serializable {

    private String idOrigem;
    private TipoMensagem tipo;
    private boolean master;
    private InetAddress endereco;
    private int porta;
    private PublicKey chavePublica;
    private byte[] assinatura;

    public PublicKey getChavePublica() {
        return chavePublica;
    }

    public void setChavePublica(PublicKey chavePublica) {
        this.chavePublica = chavePublica;
    }

    public enum TipoMensagem { DISCOVER, DISCOVER_RESP, PILA_TRANSF }

    public String getIdOrigem() {
        return idOrigem;
    }

    public void setIdOrigem(String idOrigem) {
        this.idOrigem = idOrigem;
    }

    public TipoMensagem getTipo() {
        return tipo;
    }

    public void setTipo(TipoMensagem tipo) {
        this.tipo = tipo;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public InetAddress getEndereco() {
        return endereco;
    }

    public void setEndereco(InetAddress endereco) {
        this.endereco = endereco;
    }

    public int getPorta() {
        return porta;
    }

    public void setPorta(int porta) {
        this.porta = porta;
    }

    public byte[] getAssinatura() {
        return assinatura;
    }

    public void setAssinatura(byte[] assinatura) {
        this.assinatura = assinatura;
    }
}
