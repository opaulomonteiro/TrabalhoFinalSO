package TokenRing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageController implements Runnable {

    private MessageQueue queue;
    /*Tabela de roteamento */
    private InetAddress IPAddress;
    private int port;
    private Semaphore WaitForMessage;
    private String nickname;
    private int time_token;
    private Boolean token;
    private String sentence;

    private static final String ACK = "4067";
    private static final String TOKEN = "4060";
    private static final String MSG_DADOS = "4066";

    public MessageController(MessageQueue q, String ip_port, int t_token, Boolean t, String n) throws UnknownHostException {
        queue = q;
        String aux[] = ip_port.split(":");
        IPAddress = InetAddress.getByName(aux[0]);
        port = Integer.parseInt(aux[1]);
        time_token = t_token;
        token = t;
        nickname = n;
        WaitForMessage = new Semaphore(0);
    }

    /**
     * ReceiveMessage() Nesta função, vc deve decidir o que fazer com a mensagem
     * * recebida do vizinho da esquerda: Se for um token, é a sua chance de
     * enviar uma mensagem de sua fila (queue); Se for uma mensagem de dados e
     * se for para esta estação, apenas a exiba no console, senão, envie para
     * seu vizinho da direita; Se for um ACK e se for para você, sua mensagem
     * foi enviada com sucesso, passe o token para o vizinho da direita, senão,
     * repasse o ACK para o seu vizinho da direita.
     */
    public void ReceivedMessage(String msg) throws IOException {

        if (msg.contains(TOKEN)) {
            System.out.println("\n TOKEN RECEIVED: " + msg);
            token = true;
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("\n Digite uma menssagem: ");
            sentence = inFromUser.readLine();
            String messageToSend = buildDadosMessage(sentence, "Bob");
            queue.addLocalMessage(messageToSend);
        }

        if (msg.contains(ACK)) {
            System.out.println("\n ACK Recebido: " + msg);
            //Posição 0 = Identificador de ACK
            //Posição 1 = Apelido Destino
            String[] camposDaMensagem = msg.split(";");

            // a aplicação deve verificar se esse ACK é para ela (olhando o apelido que veio no ACK).            
            if (itsForMe(camposDaMensagem[1])) {
                System.out.println("\n Confirmação do ACK, passando TOKEN para proxima estação");
                //Caso o ACK seja para ela, um token deve ser enviado para seu vizinho da direita.
                queue.addLocalMessage(TOKEN);
            } else {
                System.out.println("\n Encaminhando ack para proxima estação");
                //Caso não seja, esta mensagem deve ser enviada para seu vizinho da direita
                queue.addNetWorkMessage(msg);
            }
        }

        if (msg.contains(MSG_DADOS)) {
            System.out.println("\n Mensagem de dados recebida: " + msg);
            //Posição 0 = Identificador de msg
            //Posição 1 = Apelido Origem
            //Posição 2 = Apelido Destino
            //Posição 3 = Mensagem
            String[] camposDaMensagem = msg.split(";");

            if (itsForMe(camposDaMensagem[2])) {
                //a aplicação deve imprimir o apelido origem e a mensagem
                System.out.println("\n Origem: " + camposDaMensagem[1] + " Mensagem: " + camposDaMensagem[3]);

                // e deve também enviar uma mensagem de ACK de volta
                String ackMessage = buildAckMessage(camposDaMensagem[1]);
                System.out.println("\n Enviando msg de ACK:" + ackMessage);
                queue.addNetWorkMessage(ackMessage);
            } else {
                //esta mensagem deve ser enviada para seu vizinho da direita
                System.out.println("\n Encaminhando msg de dados para proxima estação");
                queue.addNetWorkMessage(msg);
            }
        }

        /* Libera a thread para execução. */
        WaitForMessage.release();
    }

    @Override
    public void run() {
        DatagramSocket clientSocket = null;
        byte[] sendData;

        /* Cria socket para envio de mensagem */
        try {
            clientSocket = new DatagramSocket();
        } catch (SocketException ex) {
            Logger.getLogger(MessageController.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        while (true) {

            /* Neste exemplo, considera-se que a estação sempre recebe o token 
               e o repassa para a próxima estação. */
            try {
                /* Espera time_token segundos para o envio do token. Isso é apenas para depuração,
                   durante execução real faça time_token = 0,*/
                Thread.sleep(time_token * 1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(MessageController.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (token) {
                String msg = "";
                if (queue.isLocalQueueEmpty()) {
                    msg = TOKEN;
                    sendData = TOKEN.getBytes();
                } else {
                    msg = queue.removeMessageLocal();
                    sendData = msg.getBytes();
                }

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);

                /* Realiza envio da mensagem. */
                try {
                    if (msg.equals(TOKEN)) {
                        token = false;
                    }

                    System.out.print("\n To COM o TOKEN , Enviando msg para maquina do lado " + new String(sendPacket.getData()));

                    clientSocket.send(sendPacket);
                } catch (IOException ex) {
                    Logger.getLogger(MessageController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (!token) {
                String msg = "";
                if (!queue.isNetWorkQueueEmpty()) {
                    msg = queue.removeNetWorkMessage();
                    sendData = msg.getBytes();

                    System.out.print("\n To Sem o TOKEN , Enviando msg para maquina do lado" + msg);

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);

                    /* Realiza envio da mensagem. */
                    try {
                        if (msg.equals(TOKEN)) {
                            token = false;
                        }
                        clientSocket.send(sendPacket);
                    } catch (IOException ex) {
                        Logger.getLogger(MessageController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            /* A estação fica aguardando a ação gerada pela função ReceivedMessage(). */
            try {
                WaitForMessage.acquire();
            } catch (InterruptedException ex) {
                Logger.getLogger(MessageController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private boolean itsForMe(String apelidoNaMsg) {
        return apelidoNaMsg.trim().equals(nickname);
    }

    private String buildAckMessage(String apelido) {
        return ACK + ";" + apelido;
    }

    private String buildDadosMessage(String message, String apelidoDestino) {
        return MSG_DADOS + ";" + nickname + ";" + apelidoDestino + ";" + message;
    }
}
