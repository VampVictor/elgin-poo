import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;

public class Main {
    public static void main(String[] args) {
        try (Entrada entrada = new Entrada()) {
            executar(entrada, new Usuario("admin", "1234"), new Impressora());
        }
    }

    static void executar(Entrada entrada, Usuario usuario, Impressora impressora) {
        try {
            if (!login(entrada, usuario)) {
                return;
            }
            boolean continuar = true;
            while (continuar) {
                mostrarMenu(impressora);
                int opcao = entrada.inteiro("Escolha: ", 0, 14);

                // Esta verificação ocorre ANTES de perguntar o conteúdo ou arquivo.
                if (opcao >= 4 && opcao <= 13 && !impressora.exigirConexao()) {
                    continue;
                }
                try {
                    switch (opcao) {
                        case 0 -> continuar = false;
                        case 1 -> configurar(entrada, impressora);
                        case 2 -> impressora.abrirConexao();
                        case 3 -> impressora.fecharConexao();
                        case 4 -> {
                            String texto = entrada.texto("Texto (uma linha, até 500 caracteres): ", 500);
                            impressora.imprimirTexto(texto);
                        }
                        case 5 -> {
                            String dados = entrada.texto("Conteúdo do QR Code (até 200 caracteres): ", 200);
                            impressora.imprimirQrCode(dados);
                        }
                        case 6 -> impressora.imprimirCodigoBarras("012345678912");
                        case 7 -> impressora.imprimirXmlSat(entrada.arquivo("XMLSAT.xml"));
                        case 8 -> impressora.imprimirXmlCancelamento(entrada.arquivo("CANC_SAT.xml"));
                        case 9 -> {
                            System.out.println("É necessária uma gaveta compatível conectada na porta de gaveta da impressora.");
                            impressora.abrirGaveta();
                        }
                        case 10 -> impressora.sinalSonoro();
                        case 11 -> impressora.avancarPapel();
                        case 12 -> impressora.consultarPapel();
                        case 13 -> impressora.cortarPapel();
                        case 14 -> {
                            System.out.println("SO: " + System.getProperty("os.name"));
                            System.out.println("Java: " + System.getProperty("java.version"));
                            System.out.println("Arquitetura da JVM: " + System.getProperty("os.arch"));
                            System.out.println("Working directory: " + Path.of("").toAbsolutePath());
                            System.out.println("Configuração: " + impressora.resumo());
                        }
                        default -> throw new IllegalStateException("Opção inesperada.");
                    }
                } catch (IOException e) {
                    System.out.println("Erro ao ler arquivo XML: " + e.getMessage());
                } catch (SAXException | ParserConfigurationException e) {
                    System.out.println("Erro de leitura/estrutura XML: " + e.getMessage());
                } catch (RuntimeException e) {
                    if (e instanceof NoSuchElementException) {
                        throw e;
                    }
                    System.out.println("Não foi possível executar: " + e.getMessage());
                } catch (LinkageError e) {
                    System.out.println("Erro de DLL/JNA: " + e.getMessage());
                    System.out.println("Corrija o ambiente e reinicie o programa.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("Entrada encerrada. Finalizando.");
        } finally {
            if (impressora.isConexaoAberta()) {
                System.out.println("Fechando a conexão ao sair...");
                try {
                    impressora.fecharConexao();
                } catch (RuntimeException | LinkageError e) {
                    System.out.println("Falha no fechamento: " + e.getMessage());
                }
            }
            System.out.println("Programa encerrado.");
        }
    }

    private static boolean login(Entrada entrada, Usuario usuario) {
        System.out.println("=== CONTROLE ELGIN - LOGIN ===");
        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            String login = entrada.linha("Usuário: ");
            String senha = entrada.linha("Senha: ");
            if (usuario.autenticar(login, senha)) {
                System.out.println("Login realizado.");
                return true;
            }
            System.out.println("Usuário ou senha incorretos. Tentativas restantes: " + (3 - tentativa));
        }
        System.out.println("Três tentativas incorretas. Acesso encerrado.");
        return false;
    }

    private static void configurar(Entrada entrada, Impressora impressora) {
        if (impressora.isConexaoAberta()) {
            System.out.println("Feche a conexão antes de reconfigurar.");
            return;
        }
        int tipo = entrada.inteiro("Tipo (1 USB / 2 serial / 3 rede / 4 Bluetooth): ", 1, 4);
        String modelo = entrada.texto("Modelo exato (i8 ou i9): ", 2);
        String conexao;
        int parametro;
        if (tipo == 1) {
            conexao = entrada.texto("Conexão USB (digite USB): ", 20);
            parametro = entrada.inteiro("Parâmetro USB (digite 0): ", 0, 0);
        } else if (tipo == 2) {
            conexao = entrada.texto("Porta real no Windows (ex.: COM3): ", 20);
            parametro = entrada.inteiro("Baudrate configurado na impressora: ", 1, Integer.MAX_VALUE);
        } else if (tipo == 3) {
            conexao = entrada.texto("IP/endereço da impressora: ", 253);
            parametro = entrada.inteiro("Porta TCP configurada (1..65535): ", 1, 65535);
        } else {
            conexao = entrada.texto("MAC Bluetooth (AA:BB:CC:DD:EE:FF): ", 17);
            parametro = entrada.inteiro("Parâmetro Bluetooth (digite 0): ", 0, 0);
        }
        impressora.configurar(tipo, modelo, conexao, parametro);
    }

    private static void mostrarMenu(Impressora impressora) {
        System.out.println("\n=== MENU ELGIN | conexão " + (impressora.isConexaoAberta() ? "ABERTA" : "FECHADA") + " ===");
        System.out.println("1 Configurar conexão       2 Abrir conexão");
        System.out.println("3 Fechar conexão           4 Imprimir texto");
        System.out.println("5 Imprimir QR Code         6 Imprimir código de barras");
        System.out.println("7 Imprimir XML SAT         8 Imprimir cancelamento SAT");
        System.out.println("9 Abrir gaveta            10 Sinal sonoro");
        System.out.println("11 Avançar papel          12 Status do papel");
        System.out.println("13 Cortar papel           14 Diagnóstico do ambiente");
        System.out.println("0 Sair");
    }
}
