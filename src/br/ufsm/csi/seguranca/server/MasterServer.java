package br.ufsm.csi.seguranca.server;

import br.ufsm.csi.seguranca.pila.model.Mensagem;
import br.ufsm.csi.seguranca.pila.model.MensagemFragmentada;
import br.ufsm.csi.seguranca.pila.model.ObjetoTroca;
import br.ufsm.csi.seguranca.pila.model.PilaCoin;
import br.ufsm.csi.seguranca.pilacoin.PilaDHTServer;
import br.ufsm.csi.seguranca.server.model.UsuarioServer;
import br.ufsm.csi.seguranca.util.RSAUtil;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.*;

public class MasterServer {

    private long ID_PILA = 0;

    public MasterServer() throws IOException {
    }

    private synchronized Long getID() {
        return ++ID_PILA;
    }

    private Map<InetAddress, UsuarioServer> usuarios = new HashMap<>();
    private Map<Long, PilaCoin> pilas = new HashMap<>();
    private PilaDHTServer pilaDHTServer = new PilaDHTServer();

    public static void main(String[] args) throws IOException {
        new MasterServer().iniciaThreads();
    }

    private void iniciaThreads() {
        new Thread(new TCPServer()).start();
        new Thread(new UDPServer()).start();
        new Thread(new ShowStatus()).start();
    }

    private class TCPServer implements Runnable {

        @Override
        public void run() {
            try {
                ServerSocket ss = new ServerSocket(4444);
                System.out.println("[TCP Server] Ouvindo porta 4444.");
                while (true) {
                    try {
                        Socket s = ss.accept();
                        System.out.println("[TCP Server] Recebida conexão de " + s.getInetAddress());
                        ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                        ObjetoTroca objetoTroca = (ObjetoTroca) in.readObject();
                        SecretKey chaveSessao = decifraChaveSessao(objetoTroca.getChaveSessao());
                        Cipher cipherAES = Cipher.getInstance("AES");
                        cipherAES.init(Cipher.DECRYPT_MODE, chaveSessao);
                        UsuarioServer usuario = null;
                        synchronized (MasterServer.this) {
                            usuario = usuarios.get(s.getInetAddress());
                        }
                        if (usuario == null) {
                            System.out.println("[TCP Server] Usuário desconhecido " + s.getInetAddress());
                            Mensagem resposta = new Mensagem();
                            resposta.setTipo(Mensagem.TipoMensagem.ERRO);
                            resposta.setChavePublica(RSAUtil.getMasterPublicKey());
                            resposta.setEndereco(getLocalHost());
                            resposta.setPorta(4444);
                            resposta.setMaster(true);
                            resposta.setErro("Usuário não reconhecido. Não houve mensagem DISCOVER anterior.");
                            assinaMensagem(resposta);
                            out.writeObject(resposta);
                            s.close();
                            continue;
                        }

                        byte[] bPila = cipherAES.doFinal(objetoTroca.getObjetoSerializadoCriptografado());
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] hashPila = digest.digest(bPila);
                        Cipher cipherRSA = Cipher.getInstance("RSA");
                        try {
                            cipherRSA.init(Cipher.DECRYPT_MODE, usuario.getChavePublica());
                        } catch (InvalidKeyException e) {
                            e.printStackTrace();
                            Mensagem resposta = new Mensagem();
                            resposta.setTipo(Mensagem.TipoMensagem.ERRO);
                            resposta.setChavePublica(RSAUtil.getMasterPublicKey());
                            resposta.setEndereco(getLocalHost());
                            resposta.setPorta(4444);
                            resposta.setMaster(true);
                            resposta.setErro("[TCP Server] Pila inválido: usuário sem chave pública registrada. ");
                            assinaMensagem(resposta);
                            out.writeObject(resposta);
                            s.close();
                            throw e;
                        }
                        byte[] hashAssinatura = new byte[0];
                        try {
                            hashAssinatura = cipherRSA.doFinal(objetoTroca.getAssinatura());
                        } catch (Exception e) {
                            e.printStackTrace();
                            Mensagem resposta = new Mensagem();
                            resposta.setTipo(Mensagem.TipoMensagem.ERRO);
                            resposta.setChavePublica(RSAUtil.getMasterPublicKey());
                            resposta.setEndereco(getLocalHost());
                            resposta.setPorta(4444);
                            resposta.setMaster(true);
                            resposta.setErro("[TCP Server] Pila inválido: assinatura do objetotroca inválida.");
                            assinaMensagem(resposta);
                            out.writeObject(resposta);
                            s.close();
                            throw e;
                        }
                        if (Arrays.equals(hashPila, hashAssinatura)) {
                            PilaCoin pilaCoin = (PilaCoin) deserializaObjeto(bPila);
                            String msgErro;
                            if ((msgErro = validaPilaCriado(hashPila, pilaCoin, s.getInetAddress())) == null) {
                                pilaCoin.setId(getID());
                                assinaPila(pilaCoin);
                                pilas.put(pilaCoin.getNumeroMagico(), pilaCoin);
                                pilaDHTServer.publicaPilaCoin(pilaCoin, usuario);
                                objetoTroca = criaObjetoTroca(chaveSessao, pilaCoin);
                                out.writeObject(objetoTroca);
                                s.close();
                                System.out.println("[TCP Server] Pila coin " + pilaCoin.getId() + " válido recebido do usuário " + pilaCoin.getIdCriador() + ".");
                            } else {
                                Mensagem resposta = new Mensagem();
                                resposta.setTipo(Mensagem.TipoMensagem.ERRO);
                                resposta.setChavePublica(RSAUtil.getMasterPublicKey());
                                resposta.setEndereco(getLocalHost());
                                resposta.setPorta(4444);
                                resposta.setMaster(true);
                                resposta.setErro("[TCP Server] Pila inválido: " + msgErro);
                                assinaMensagem(resposta);
                                out.writeObject(resposta);
                                s.close();
                            }
                        } else {
                            System.out.println("[TCP Server] Assinatura inválida " + s.getInetAddress());
                            Mensagem resposta = new Mensagem();
                            resposta.setTipo(Mensagem.TipoMensagem.ERRO);
                            resposta.setChavePublica(RSAUtil.getMasterPublicKey());
                            resposta.setEndereco(getLocalHost());
                            resposta.setPorta(4444);
                            resposta.setMaster(true);
                            resposta.setErro("Assinatura inválida.");
                            assinaMensagem(resposta);
                            out.writeObject(resposta);
                            s.close();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private ObjetoTroca criaObjetoTroca(SecretKey chaveSessao, PilaCoin pilaCoin) throws Exception {
        ObjetoTroca objetoTroca = new ObjetoTroca();
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, chaveSessao);
        byte[] objeto = serializaObjeto(pilaCoin);
        byte[] objetoCripto = cipher.doFinal(objeto);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hash = messageDigest.digest(objeto);
        Cipher cipherRSA = Cipher.getInstance("RSA");
        cipherRSA.init(Cipher.ENCRYPT_MODE, RSAUtil.getPrivateKey("Master_private_key.der"));
        objetoTroca.setAssinatura(cipherRSA.doFinal(hash));
        objetoTroca.setObjetoSerializadoCriptografado(objetoCripto);
        return objetoTroca;
    }

    private String validaPilaCriado(byte[] hashPila, PilaCoin pilaCoin, InetAddress endereco) {
        UsuarioServer usuario = null;
        synchronized (MasterServer.this) {
            usuario = usuarios.get(endereco);
        }
        if (pilas.get(pilaCoin.getNumeroMagico()) != null) {
            return "Número mágico já utilizado.";
        }
        if (usuario != null) {
            if (usuario.getChavePublica().equals(pilaCoin.getChaveCriador())) {
                BigInteger bigInteger = new BigInteger(1, hashPila);
                if (bigInteger.compareTo(new BigInteger("99999998000000000000000000000000000000000000000000000000000000000000000")) < 0) {
                    usuario.setValidacaoPilaOk(true);
                    return null;
                } else {
                    return "Número mágico inválido: gerou hash " + bigInteger;
                }
            } else {
                return "Chave pública do usuário diferente da chave pública do criador do pila coin.";
            }
        } else {
            return "Usuário inválido";
        }
    }

    private SecretKey decifraChaveSessao(byte[] chaveSessao) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, RSAUtil.getPrivateKey("Master_private_key.der"));
        byte[] chave = cipher.doFinal(chaveSessao);
        return new SecretKeySpec(chave, "AES");
    }

    private InetAddress localhost;

    private synchronized InetAddress getLocalHost() throws SocketException {
        if (localhost == null) {
            InetAddress address = null;
            Enumeration en = NetworkInterface.getNetworkInterfaces();
            while(en.hasMoreElements())
            {
                NetworkInterface n = (NetworkInterface) en.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements())
                {
                    InetAddress i = (InetAddress) ee.nextElement();
                    if (i.isSiteLocalAddress() && !i.isLoopbackAddress()) {
                        address = i;
                    }
                }
            }
            localhost = address;
        }
        return localhost;
    }

    private class ShowStatus implements Runnable {

        @Override
        public void run() {
            while (true) {
                Scanner scanner = new Scanner(System.in);
                String s = scanner.nextLine();
                if (s != null && s.trim().contains("s")) {
                    System.out.println("USUÁRIO\t|\tENDERECO\t\t|\tDISCOVER\t|\tVALID. PILA\t|\tPILA_TRANSF");
                    Collection<UsuarioServer> cusuarios = null;
                    synchronized (MasterServer.this) {
                        cusuarios = usuarios.values();
                    }
                    for (UsuarioServer usuario : cusuarios) {
                        System.out.print(usuario.getId());
                        System.out.print("\t|\t");
                        System.out.print(usuario.getEndereco());
                        System.out.print("\t|\t");
                        System.out.print(usuario.isMsgDiscoverOk());
                        System.out.print("\t|\t");
                        System.out.print(usuario.isValidacaoPilaOk());
                        System.out.print("\t\t|\t");
                        System.out.println(usuario.isPilaTransfOk());
                    }
                    System.out.println("---------------- FIM DA TABELA ----------------------");
                }
            }
        }
    }

    private class UDPServer implements Runnable {

        @Override
        public void run() {
            byte[] receiveData = new byte[1500];
            try {
                System.out.println("IP: " + getLocalHost());
                System.out.println("[UDP Server] Escutando porta 3333.");
                DatagramSocket serverSocket = new DatagramSocket(3333);
                while (true) {
                    try {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);
                        ByteArrayInputStream in = new ByteArrayInputStream(receivePacket.getData());
                        ObjectInputStream objectInputStream = new ObjectInputStream(in);
                        Mensagem mensagem = null;
                        MensagemFragmentada msgFragmentada = null;
                        try {
                            Object o = objectInputStream.readObject();
                            if (o instanceof Mensagem) {
                                mensagem = (Mensagem) o;
                            } else if (o instanceof MensagemFragmentada) {
                                msgFragmentada = (MensagemFragmentada) o;
                            } else {
                                System.out.println("[UDP Server] Pacote inválido recebido de " + receivePacket.getAddress() +
                                        " (" + o.getClass() + ").");
                                continue;
                            }
                        } catch (ClassNotFoundException | java.io.InvalidClassException e) {
                            //e.printStackTrace();
                            System.out.println("[UDP Server] Pacote inválido recebido de " + receivePacket.getAddress() +
                                    " (" + e.getMessage() + ").");
                            continue;
                        }
                        synchronized (MasterServer.this) {
                            if (msgFragmentada != null) {
                                UsuarioServer u = usuarios.get(receivePacket.getAddress());
                                if (u != null) {
                                    u.setPilaTransfOk(true);
                                }
                            } else {
                                System.out.println("[UDP Server] Recebido pacote tipo " + mensagem.getTipo() + " de " +
                                        receivePacket.getAddress() + " (id = " + mensagem.getIdOrigem() + ") .");
                                if (mensagem.getTipo() == Mensagem.TipoMensagem.DISCOVER) {
                                    UsuarioServer usuario = usuarios.get(receivePacket.getAddress());
                                    if (usuario == null) {
                                        usuario = new UsuarioServer();
                                        usuario.setEndereco(receivePacket.getAddress());
                                        usuarios.put(usuario.getEndereco(), usuario);
                                    }
                                    usuario.setMsgDiscoverOk(true);
                                    usuario.setChavePublica(mensagem.getChavePublica());
                                    usuario.setEndereco(receivePacket.getAddress());
                                    usuario.setId(mensagem.getIdOrigem());
                                    //enviando pacote de resposta
                                    Mensagem resposta = new Mensagem();
                                    resposta.setTipo(Mensagem.TipoMensagem.DISCOVER_RESP);
                                    resposta.setChavePublica(RSAUtil.getMasterPublicKey());
                                    resposta.setEndereco(getLocalHost());
                                    resposta.setPorta(4444);
                                    resposta.setMaster(true);
                                    assinaMensagem(resposta);
                                    byte[] respSerial = serializaObjeto(resposta);
                                    DatagramPacket sendPacket =
                                            new DatagramPacket(respSerial, respSerial.length, receivePacket.getAddress(), mensagem.getPorta());
                                    serverSocket.send(sendPacket);
                                    System.out.println("[UDP Server] Enviado " + resposta.getTipo() + " para " + receivePacket.getAddress() + ".");
                                } else if (mensagem.getTipo() == Mensagem.TipoMensagem.PILA_TRANSF) {
                                    UsuarioServer u = usuarios.get(receivePacket.getAddress());
                                    if (u != null && mensagem.getPilaCoin() != null && mensagem.getPilaCoin().getTransacoes() != null &&
                                            !mensagem.getPilaCoin().getTransacoes().isEmpty()) {
                                        u.setPilaTransfOk(true);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void assinaMensagem(Mensagem mensagem) throws Exception {
        byte[] mSer = serializaObjeto(mensagem);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(mSer);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, RSAUtil.getPrivateKey("./Master_private_key.der"));
        mensagem.setAssinatura(cipher.doFinal(hash));
    }

    private void assinaPila(PilaCoin pila) throws Exception {
        byte[] mSer = serializaObjeto(pila);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(mSer);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, RSAUtil.getPrivateKey("./Master_private_key.der"));
        pila.setAssinaturaMaster(cipher.doFinal(hash));
    }


    public static byte[] serializaObjeto(Serializable obj) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(obj);
        return bout.toByteArray();
    }

    public static Serializable deserializaObjeto(byte[] obj) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bin = new ByteArrayInputStream(obj);
        ObjectInputStream in = new ObjectInputStream(bin);
        return (Serializable) in.readObject();
    }

    private void foo() throws IOException {
        PilaCoin pilaCoin = new PilaCoin();
        //pilaCoin.set
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(byteArrayOutputStream);
        oout.writeObject(pilaCoin);
        byte[] pilaserializado = byteArrayOutputStream.toByteArray();


    }


}
