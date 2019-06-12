package br.ufsm.csi.seguranca.server.model;

import br.ufsm.csi.seguranca.pila.model.Usuario;

import java.io.Serializable;

/**
 * Created by cpol on 19/04/2018.
 */
public class UsuarioServer extends Usuario implements Serializable {

    private static final long serialVersionUID = 2L;
    private boolean msgDiscoverOk;
    private boolean validacaoPilaOk;
    private boolean pilaTransfOk;

    public boolean isMsgDiscoverOk() {
        return msgDiscoverOk;
    }

    public void setMsgDiscoverOk(boolean msgDiscoverOk) {
        this.msgDiscoverOk = msgDiscoverOk;
    }

    public boolean isValidacaoPilaOk() {
        return validacaoPilaOk;
    }

    public void setValidacaoPilaOk(boolean validacaoPilaOk) {
        this.validacaoPilaOk = validacaoPilaOk;
    }

    public boolean isPilaTransfOk() {
        return pilaTransfOk;
    }

    public void setPilaTransfOk(boolean pilaTransfOk) {
        this.pilaTransfOk = pilaTransfOk;
    }

}
