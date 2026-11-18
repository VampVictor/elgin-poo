import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;

/** Testes da lógica com biblioteca falsa. Não comprovam impressão física. */
public class TestesSemHardware {
    private static int verificacoes;

    public static void main(String[] args) throws Exception {
        String resultado = capturar(() -> {
            testarLoginEMenu();
            testarConexaoERetornos();
            testarValidacaoEXml();
            testarFluxoCompleto();
            testarCorteAutomatico();
            testarPadroesDoMenu();
        });
        Files.createDirectories(Path.of("build"));
        Files.writeString(Path.of("build", "saida-testes.txt"), resultado, StandardCharsets.UTF_8);
        System.out.println("APROVADO: " + verificacoes + " verificações da lógica Java, sem hardware.");
        System.out.println("Log: build/saida-testes.txt. DLL Windows e impressora física NÃO executadas.");
    }

    private static void testarLoginEMenu() throws Exception {
        BibliotecaFalsa falsa = new BibliotecaFalsa();
        String log = executarMenu("x\nx\nx\nx\nx\nx\n", new Impressora(falsa));
        verificar(contar(log, "Usuário: ") == 3, "Login deve pedir exatamente 3 vezes");
        verificar(log.contains("restantes: 2") && log.contains("restantes: 1")
                && log.contains("restantes: 0"), "Tentativas restantes");
        verificar(!log.contains("=== MENU"), "Não deve mostrar menu após 3 falhas");
        verificar(falsa.chamadas.isEmpty(), "Login inválido não toca a DLL");

        log = executarMenu("x\nx\nx\nx\nadmin\n1234\n0\n", new Impressora(falsa));
        verificar(log.contains("Login realizado."), "Login na terceira tentativa");

        StringBuilder entrada = new StringBuilder("admin\n1234\nabc\n99999999999999999999\n-1\n");
        for (int opcao = 4; opcao <= 13; opcao++) {
            entrada.append(opcao).append('\n');
        }
        entrada.append("0\n");
        log = executarMenu(entrada.toString(), new Impressora(falsa));
        verificar(contar(log, "Conexão fechada. Abra a conexão antes.") == 10,
                "Todas as operações físicas bloqueadas antes de perguntar dados");
        verificar(!log.contains("Texto (uma linha") && !log.contains("Arquivo [Enter")
                && !log.contains("Quantidade de bips ("), "Sem perguntas indevidas");
        verificar(log.contains("Entrada inválida") && log.contains("Informe um número"),
                "Letras, estouro de inteiro e valores fora da faixa");
        verificar(falsa.chamadas.isEmpty(), "Menu fechado não chama DLL");
        log = executarMenu("admin\n1234\n", new Impressora(falsa));
        verificar(log.contains("Entrada encerrada"), "Fim da entrada encerra sem travar");
    }

    private static void testarConexaoERetornos() throws Exception {
        BibliotecaFalsa falsa = new BibliotecaFalsa();
        Impressora impressora = new Impressora(falsa);
        verificar(!impressora.abrirConexao(), "Sem configuração não abre");
        impressora.configurar(1, "I9", "USB", 0);
        falsa.retornos.put("AbreConexaoImpressora", -21);
        verificar(!impressora.abrirConexao() && !impressora.isConexaoAberta(), "Abertura falhou");
        int antes = falsa.chamadas.size();
        verificar(!impressora.imprimirTexto("T", 0, 0, 0, 1), "Impressão bloqueada após falha");
        verificar(falsa.chamadas.size() == antes, "Não envia texto após falha de abertura");
        falsa.retornos.put("AbreConexaoImpressora", 0);
        verificar(impressora.abrirConexao() && impressora.isConexaoAberta(), "Abertura com zero");
        Object[] abertura = falsa.argumentos.get("AbreConexaoImpressora");
        verificar(abertura[0].equals(1) && abertura[1].equals("i9") && abertura[2].equals("USB")
                && abertura[3].equals(0), "Parâmetros USB exatos");
        antes = falsa.chamadas.size();
        verificar(!impressora.abrirConexao() && falsa.chamadas.size() == antes, "Não abre duas vezes");
        esperarErro(() -> impressora.configurar(1, "i8", "USB", 0), "Não reconfigura aberta");

        falsa.retornos.put("ImpressaoTexto", 64);
        verificar(impressora.imprimirTexto("Olá ação", 1, 8, 0, 2), "Retorno positivo no texto é sucesso");
        verificar(contar(falsa.chamadas, "ImpressaoTexto") == 2, "Duas cópias em laço");
        verificar(falsa.argumentos.get("ImpressaoTexto")[0].equals("Olá ação\n"), "LF no fim do texto");
        falsa.retornos.put("ImpressaoTexto", -44);
        antes = contar(falsa.chamadas, "AvancaPapel");
        verificar(!impressora.imprimirTexto("T", 0, 0, 0, 3), "Texto negativo interrompe cópias");
        verificar(contar(falsa.chamadas, "AvancaPapel") == antes, "Não avança após falha de texto");
        falsa.retornos.put("ImpressaoTexto", 0);
        verificar(impressora.imprimirTexto("T", 0, 0, 0, 1), "Texto zero aceito pelo enunciado");
        falsa.retornos.put("ImpressaoQRCode", 1);
        verificar(!impressora.imprimirQrCode("https://example.com", 4, 2), "QR exige zero");
        falsa.retornos.put("ImpressaoQRCode", 0);
        verificar(impressora.imprimirQrCode("https://example.com", 4, 2), "QR sucesso");
        verificar(impressora.imprimirCodigoBarras("012345678912"), "CODE 128 sucesso");
        verificar(falsa.argumentos.get("ImpressaoCodigoBarras")[1].equals("{A012345678912"),
                "Prefixo CODE 128 exato");
        verificar(impressora.abrirGaveta(0, 5, 10), "Gaveta alternativa permitida");
        verificar(!falsa.chamadas.contains("AbreGavetaElgin"), "Não usa assinatura divergente");
        verificar(impressora.sinalSonoro(2), "Bips");
        for (int status : new int[]{5, 6, 7}) {
            falsa.retornos.put("StatusImpressora", status);
            String log = capturar(impressora::consultarPapel);
            verificar(log.contains("Retorno: " + status), "Status positivo " + status);
        }
        falsa.retornos.put("FechaConexaoImpressora", -22);
        verificar(!impressora.fecharConexao() && impressora.isConexaoAberta(),
                "Falha ao fechar mantém estado para nova tentativa");
        falsa.retornos.put("FechaConexaoImpressora", 0);
        verificar(impressora.fecharConexao() && !impressora.isConexaoAberta(), "Fechamento normal");
        verificar(impressora.abrirConexao(), "Reabre depois de fechar");
        falsa.retornos.put("SinalSonoro", -4);
        verificar(!impressora.sinalSonoro(1) && !impressora.isConexaoAberta(), "SDK -4 fecha estado local");
    }

    private static void testarValidacaoEXml() throws Exception {
        BibliotecaFalsa falsa = new BibliotecaFalsa();
        Impressora impressora = new Impressora(falsa);
        esperarErro(() -> impressora.configurar(1, "i9", "USB", 1), "Parâmetro USB inválido");
        esperarErro(() -> impressora.configurar(2, "i9", "USB", 9600), "Porta serial inválida");
        esperarErro(() -> impressora.configurar(3, "i9", "192.168.0.20", 70000), "Porta TCP inválida");
        esperarErro(() -> impressora.configurar(4, "i9", "INCORRETO", 0), "MAC inválido");
        impressora.configurar(1, "i8", "USB", 0);
        verificar(impressora.abrirConexao(), "i8 aceita");
        esperarErro(() -> impressora.imprimirCodigoBarras("ABC"), "Barras inválidas");
        esperarErro(() -> impressora.imprimirTexto("T\u0000X", 0, 0, 0, 1), "NUL proibido");
        esperarErro(() -> impressora.imprimirTexto("T", 0, 0, 6, 1), "Altura comum a i8/i9");
        esperarErro(() -> impressora.sinalSonoro(64), "Máximo 63 bips");
        esperarErro(() -> impressora.imprimirQrCode("T", 0, 2), "QR tamanho mínimo");

        verificar(impressora.imprimirXmlSat("XMLSAT.xml"), "XML de venda fornecido passa leitura");
        String xml = (String) falsa.argumentos.get("ImprimeXMLSAT")[0];
        verificar(xml.startsWith("<?xml") && xml.contains("<CFe>"), "Envia conteúdo XML, não nome");
        verificar(impressora.imprimirXmlCancelamento("CANC_SAT.xml"), "XML cancelamento fornecido passa leitura");
        String assinaturaXml = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(Path.of("CANC_SAT.xml").toFile())
                .getElementsByTagName("assinaturaQRCODE").item(0).getTextContent();
        verificar(Impressora.ASS_QRCODE.equals(assinaturaXml), "Constante igual à assinatura do exemplo");
        verificar(falsa.argumentos.get("ImprimeXMLCancelamentoSAT")[1].equals(assinaturaXml),
                "Assinatura correta no argumento da DLL");
        falsa.retornos.put("ImprimeXMLSAT", 12);
        verificar(!impressora.imprimirXmlSat("XMLSAT.xml"), "Linha positiva de erro XML não é sucesso");
        esperarErro(() -> impressora.imprimirXmlSat("arquivo-inexistente.xml"), "Arquivo inexistente");
        esperarErro(() -> impressora.imprimirXmlSat("CANC_SAT.xml"), "XML trocado");
        Path temporario = Files.createTempFile("elgin-teste-", ".xml");
        try {
            Files.writeString(temporario, "<CFe><infCFe></CFe>");
            esperarErro(() -> impressora.imprimirXmlSat(temporario.toString()), "XML malformado");
            Files.writeString(temporario, "<!DOCTYPE CFe [<!ENTITY x SYSTEM 'file:///arquivo'>]><CFe>&x;</CFe>");
            esperarErro(() -> impressora.imprimirXmlSat(temporario.toString()), "DTD externo rejeitado");
            Files.writeString(temporario, "<CFeCanc><infCFe Id='outro'/></CFeCanc>");
            esperarErro(() -> impressora.imprimirXmlCancelamento(temporario.toString()),
                    "Outro cancelamento não usa assinatura do exemplo");
        } finally {
            Files.deleteIfExists(temporario);
        }
        impressora.fecharConexao();
    }

    private static void testarFluxoCompleto() throws Exception {
        BibliotecaFalsa falsa = new BibliotecaFalsa();
        Impressora impressora = new Impressora(falsa);
        String log = executarMenu("admin\n1234\n1\n1\ni9\nUSB\n0\n2\n"
                + "4\nTESTE\n5\nhttps://example.com\n"
                + "6\n012345678912\n7\n\n8\n\n9\n10\n11\n12\n13\n14\n0\n",
                impressora);
        verificar(!log.contains("Não foi possível executar") && !log.contains("Erro ao ler"), "Fluxo completo sem erros");
        verificar(!impressora.isConexaoAberta(), "Sair fecha a conexão");
        verificar(contar(falsa.chamadas, "FechaConexaoImpressora") == 1, "Fecha exatamente uma vez ao sair");
        verificar(log.contains("Arquitetura da JVM:"), "Diagnóstico acessível");

        falsa = new BibliotecaFalsa();
        impressora = new Impressora(falsa);
        executarMenu("admin\n1234\n1\n1\ni9\nUSB\n0\n2\n7\nnao-existe.xml\n10\n0\n", impressora);
        verificar(falsa.chamadas.contains("SinalSonoro"), "Menu continua depois de arquivo ausente");
    }

    private static void testarCorteAutomatico() throws Exception {
        BibliotecaFalsa falsa = new BibliotecaFalsa();
        Impressora impressora = new Impressora(falsa);
        impressora.configurar(1, "i9", "USB", 0);
        impressora.abrirConexao();
        falsa.chamadas.clear();
        verificar(impressora.imprimirTexto("Teste", 0, 0, 0, 2), "Duas cópias finalizadas");
        verificar(falsa.chamadas.equals(List.of("ImpressaoTexto", "AvancaPapel", "Corte",
                "ImpressaoTexto", "AvancaPapel", "Corte")), "Cada cópia imprime, avança e corta em ordem");

        List<Acao> operacoes = List.of(
                () -> verificar(impressora.imprimirQrCode("https://example.com"), "QR completo"),
                () -> verificar(impressora.imprimirCodigoBarras("123"), "Barras completas"),
                () -> verificar(impressora.imprimirXmlSat("XMLSAT.xml"), "SAT completo"),
                () -> verificar(impressora.imprimirXmlCancelamento("CANC_SAT.xml"), "Cancelamento completo"));
        List<String> comandos = List.of("ImpressaoQRCode", "ImpressaoCodigoBarras",
                "ImprimeXMLSAT", "ImprimeXMLCancelamentoSAT");
        for (int i = 0; i < operacoes.size(); i++) {
            falsa.chamadas.clear();
            operacoes.get(i).executar();
            verificar(falsa.chamadas.equals(List.of(comandos.get(i), "AvancaPapel", "Corte")),
                    "Uma finalização após " + comandos.get(i));
        }

        // Todas as impressões interrompem antes de avançar/cortar se a DLL recusar os dados.
        List<Acao> falhas = List.of(
                () -> verificar(!impressora.imprimirTexto("Teste"), "Falha no texto"),
                () -> verificar(!impressora.imprimirQrCode("Teste"), "Falha no QR"),
                () -> verificar(!impressora.imprimirCodigoBarras("123"), "Falha nas barras"),
                () -> verificar(!impressora.imprimirXmlSat("XMLSAT.xml"), "Falha no SAT"),
                () -> verificar(!impressora.imprimirXmlCancelamento("CANC_SAT.xml"), "Falha no cancelamento"));
        List<String> nomes = List.of("ImpressaoTexto", "ImpressaoQRCode", "ImpressaoCodigoBarras",
                "ImprimeXMLSAT", "ImprimeXMLCancelamentoSAT");
        for (int i = 0; i < falhas.size(); i++) {
            falsa.chamadas.clear();
            String nome = nomes.get(i);
            falsa.retornos.put(nome, nome.startsWith("ImprimeXML") ? 12 : -44);
            falhas.get(i).executar();
            verificar(falsa.chamadas.equals(List.of(nome)), "Falha não dispara corte: " + nome);
            falsa.retornos.remove(nome);
        }

        falsa.chamadas.clear();
        falsa.retornos.put("AvancaPapel", -44);
        verificar(!impressora.imprimirTexto("Teste"), "Falha ao avançar interrompe finalização");
        verificar(falsa.chamadas.equals(List.of("ImpressaoTexto", "AvancaPapel")), "Não corta após falha no avanço");
        falsa.retornos.remove("AvancaPapel");
        falsa.chamadas.clear();
        falsa.retornos.put("Corte", -44);
        verificar(!impressora.imprimirTexto("Teste", 0, 0, 0, 2), "Falha no corte impede segunda cópia");
        verificar(falsa.chamadas.equals(List.of("ImpressaoTexto", "AvancaPapel", "Corte")), "Corte não é repetido automaticamente");
        falsa.chamadas.clear();
        String log = executarMenu("admin\n1234\n5\nTeste\n10\n0\n", impressora);
        verificar(log.contains("corte falhou"), "Menu avisa que o conteúdo já foi enviado");
        verificar(falsa.chamadas.equals(List.of("ImpressaoQRCode", "AvancaPapel", "Corte",
                "SinalSonoro", "FechaConexaoImpressora")), "Após falha no corte o menu continua, sem reimprimir");
        falsa.retornos.remove("Corte");

        // Ações sem impressão não desperdiçam papel e corte manual corta só uma vez.
        impressora.abrirConexao();
        falsa.chamadas.clear();
        impressora.abrirGaveta();
        impressora.sinalSonoro();
        impressora.avancarPapel();
        impressora.consultarPapel();
        impressora.cortarPapel();
        impressora.fecharConexao();
        verificar(falsa.chamadas.equals(List.of("AbreGaveta", "SinalSonoro", "AvancaPapel",
                "StatusImpressora", "Corte", "FechaConexaoImpressora")), "Controles não recebem cortes extras");
    }

    private static void testarPadroesDoMenu() throws Exception {
        BibliotecaFalsa falsa = new BibliotecaFalsa();
        Impressora impressora = new Impressora(falsa);
        String log = executarMenu("admin\n1234\n1\n1\ni9\nUSB\n0\n2\n"
                + "4\nAção na escola\n5\nhttps://example.com\n6\n012345678912\n"
                + "7\n\n8\n\n9\n10\n11\n12\n13\n14\n0\n", impressora);
        verificar(!log.contains("Entrada encerrada") && !log.contains("Não foi possível executar")
                && !log.contains("Entrada inválida"), "Menu consome apenas conteúdo/arquivo nas impressões");
        for (String pergunta : List.of("Alinhamento (", "Estilo (", "Altura (", "Cópias (", "Tamanho (",
                "Correção (", "Pino conforme", "Tempo inicial", "Tempo final", "Quantidade de bips (",
                "Linhas (", "Avanço antes do corte (")) {
            verificar(!log.contains(pergunta), "Menu não pede parâmetro técnico: " + pergunta);
        }
        verificar(java.util.Arrays.equals(falsa.argumentos.get("ImpressaoTexto"),
                new Object[]{"Ação na escola\n", 0, 0, 0}), "Texto padrão preserva acentos e usa formato normal");
        verificar(java.util.Arrays.equals(falsa.argumentos.get("ImpressaoQRCode"),
                new Object[]{"https://example.com", 4, 2}), "QR usa tamanho 4 e correção 2");
        verificar(java.util.Arrays.equals(falsa.argumentos.get("ImpressaoCodigoBarras"),
                new Object[]{8, "{A012345678912", 100, 2, 2}), "Barras preservam zeros e parâmetros legíveis");
        verificar(java.util.Arrays.equals(falsa.argumentos.get("SinalSonoro"),
                new Object[]{2, 50, 5}), "Sinal sonoro usa dois bips sem pergunta");
        verificar(java.util.Arrays.equals(falsa.argumentos.get("AbreGaveta"),
                new Object[]{0, 50, 50}), "Gaveta usa configuração centralizada");
        verificar(falsa.argumentos.get("AvancaPapel")[0].equals(3), "Avanço padrão de três linhas");
        verificar(falsa.argumentos.get("Corte")[0].equals(5), "Avanço de corte padrão");
        verificar(contar(falsa.chamadas, "Corte") == 6, "Cinco impressões e um corte manual, sem cortes duplicados");
        verificar(contar(falsa.chamadas, "ImpressaoTexto") == 1, "Uma cópia por escolha no menu");

        falsa = new BibliotecaFalsa();
        impressora = new Impressora(falsa);
        log = executarMenu("admin\n1234\n1\n1\ni9\nUSB\n0\n2\n"
                + "6\nABC\n10\n7\nnao-existe.xml\n5\nOK\n0\n", impressora);
        verificar(contar(falsa.chamadas, "Corte") == 1, "Dados/arquivos inválidos não cortam");
        verificar(falsa.chamadas.contains("SinalSonoro") && falsa.chamadas.contains("ImpressaoQRCode"),
                "Menu continua depois de dados/arquivos inválidos");
    }

    private static String executarMenu(String texto, Impressora impressora) throws Exception {
        var entradaOriginal = System.in;
        try {
            System.setIn(new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)));
            return capturar(() -> {
                try (Entrada entrada = new Entrada()) {
                    Main.executar(entrada, new Usuario("admin", "1234"), impressora);
                }
            });
        } finally {
            System.setIn(entradaOriginal);
        }
    }

    private static String capturar(Acao acao) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream saida = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(saida);
            acao.executar();
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void verificar(boolean condicao, String nome) {
        verificacoes++;
        if (!condicao) {
            throw new AssertionError(nome);
        }
    }

    private static void esperarErro(Acao acao, String nome) throws Exception {
        try {
            acao.executar();
        } catch (Exception esperado) {
            verificar(true, nome);
            return;
        }
        verificar(false, nome);
    }

    private static int contar(String texto, String trecho) {
        return (texto.length() - texto.replace(trecho, "").length()) / trecho.length();
    }

    private static int contar(List<String> lista, String item) {
        return (int) lista.stream().filter(item::equals).count();
    }

    private interface Acao { void executar() throws Exception; }

    /** Só devolve valores programados. Não imprime, não apita, não usa DLL. */
    private static class BibliotecaFalsa implements ImpressoraDLL {
        final List<String> chamadas = new ArrayList<>();
        final Map<String, Integer> retornos = new HashMap<>();
        final Map<String, Object[]> argumentos = new HashMap<>();

        int chamada(String nome, Object... args) {
            chamadas.add(nome);
            argumentos.put(nome, args);
            return retornos.getOrDefault(nome, nome.equals("ImpressaoTexto") ? 10
                    : nome.equals("StatusImpressora") ? 5 : 0);
        }

        public int AbreConexaoImpressora(int a, String b, String c, int d) { return chamada("AbreConexaoImpressora", a, b, c, d); }
        public int FechaConexaoImpressora() { return chamada("FechaConexaoImpressora"); }
        public int ImpressaoTexto(String a, int b, int c, int d) { return chamada("ImpressaoTexto", a, b, c, d); }
        public int Corte(int a) { return chamada("Corte", a); }
        public int ImpressaoQRCode(String a, int b, int c) { return chamada("ImpressaoQRCode", a, b, c); }
        public int ImpressaoCodigoBarras(int a, String b, int c, int d, int e) { return chamada("ImpressaoCodigoBarras", a, b, c, d, e); }
        public int AvancaPapel(int a) { return chamada("AvancaPapel", a); }
        public int StatusImpressora(int a) { return chamada("StatusImpressora", a); }
        public int AbreGavetaElgin(int a, int b, int c) { throw new AssertionError("Método divergente não deve ser usado"); }
        public int AbreGaveta(int a, int b, int c) { return chamada("AbreGaveta", a, b, c); }
        public int SinalSonoro(int a, int b, int c) { return chamada("SinalSonoro", a, b, c); }
        public int ModoPagina() { throw new AssertionError("Não usado"); }
        public int LimpaBufferModoPagina() { throw new AssertionError("Não usado"); }
        public int ImprimeModoPagina() { throw new AssertionError("Não usado"); }
        public int ModoPadrao() { throw new AssertionError("Não usado"); }
        public int PosicaoImpressaoHorizontal(int a) { throw new AssertionError("Não usado"); }
        public int PosicaoImpressaoVertical(int a) { throw new AssertionError("Não usado"); }
        public int ImprimeXMLSAT(String a, int b) { return chamada("ImprimeXMLSAT", a, b); }
        public int ImprimeXMLCancelamentoSAT(String a, String b, int c) { return chamada("ImprimeXMLCancelamentoSAT", a, b, c); }
    }
}
