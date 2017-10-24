package TokenRing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TokenRing {

    private static final String MSG_DADOS = "4066";

    public static void main(String[] args) throws IOException {
        String ip_port;
        int port;
        int t_token = 0;
        boolean token = false;
        String nickname;

        /* Le arquivo de configuração. */
        try (BufferedReader inputFile = new BufferedReader(new FileReader("ring.cfg"))) {

            /* Lê IP e Porta */
            ip_port = inputFile.readLine();
            String aux[] = ip_port.split(":");
            port = Integer.parseInt(aux[1]);

            /* Lê apelido */
            nickname = inputFile.readLine();

            /* Lê tempo de espera com o token. Usado para fins de depuração. Em caso de 
            execução normal use valor 0. */
            t_token = Integer.parseInt(inputFile.readLine());

            /* Lê se a estação possui o token inicial. */
            token = Boolean.parseBoolean(inputFile.readLine());

        } catch (FileNotFoundException ex) {
            Logger.getLogger(TokenRing.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        /* Cria uma fila de mensagens. */
        MessageQueue queue = new MessageQueue();

        MessageController controller = new MessageController(queue, ip_port, t_token, token, nickname);
        Thread thr_controller = new Thread(controller);
        Thread thr_receiver = new Thread(new MessageReceiver(port, controller));

        thr_controller.start();
        thr_receiver.start();

        /* Neste ponto, a thread principal deve ficar aguarando o usuário entrar com o destinatário
         * e a mensagem a ser enviada. Destinatário e mensagem devem ser adicionados na fila de mensagens pendentes.
         * MessageQueue()
         *
         */
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        String sentence = "";
        while (true) {
            System.out.print("\n Digite uma menssagem: ");
            try {
                sentence = inFromUser.readLine();
            } catch (IOException ex) {
                Logger.getLogger(MessageController.class.getName()).log(Level.SEVERE, null, ex);
            }
            String messageToSend = buildDadosMessage(sentence, nickname, "Paulo");
            queue.addLocalMessage(messageToSend);
        }

    }

    private static String buildDadosMessage(String message, String nickName, String apelidoDestino) {
        return MSG_DADOS + ";" + nickName + ";" + apelidoDestino + ";" + message;
    }

}
