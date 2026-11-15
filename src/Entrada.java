import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Lê sempre uma linha inteira: evita o problema nextInt() / nextLine(). */
public class Entrada implements AutoCloseable {
    private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

    public String linha(String pergunta) {
        System.out.print(pergunta);
        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException("Entrada encerrada.");
        }
        return scanner.nextLine();
    }

    public String texto(String pergunta, int maximo) {
        while (true) {
            String valor = linha(pergunta).trim();
            if (!valor.isEmpty() && valor.length() <= maximo
                    && valor.chars().noneMatch(Character::isISOControl)) {
                return valor;
            }
            System.out.println("Digite de 1 a " + maximo + " caracteres, sem controles.");
        }
    }

    public int inteiro(String pergunta, int minimo, int maximo) {
        while (true) {
            try {
                int valor = Integer.parseInt(linha(pergunta).trim());
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.println("Informe um número de " + minimo + " a " + maximo + ".");
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Digite um número inteiro.");
            }
        }
    }

    public String arquivo(String padrao) {
        String caminho = linha("Arquivo [Enter = " + padrao + "]: ").trim();
        if (caminho.length() >= 2 && caminho.startsWith("\"") && caminho.endsWith("\"")) {
            caminho = caminho.substring(1, caminho.length() - 1);
        }
        return caminho.isEmpty() ? padrao : caminho;
    }

    @Override
    public void close() {
        scanner.close();
    }
}