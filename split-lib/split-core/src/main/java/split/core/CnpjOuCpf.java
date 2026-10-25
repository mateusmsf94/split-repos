package split.core;

import java.util.regex.Pattern;

/** Campos que aceitam CNPJ ou CPF, como cnpjCpfPagEfet e cnpjCpfDest. Os dois formatos não se sobrepõem. */
public sealed interface CnpjOuCpf permits Cnpj, Cpf {

    Pattern FORMATO = Pattern.compile("\\d{11}|[A-Za-z0-9]{14}");

    String valor();

    static CnpjOuCpf ler(String valor) {
        Validacoes.exigir(FORMATO, valor, "CNPJ ou CPF");
        return valor.length() == 11 ? new Cpf(valor) : new Cnpj(valor);
    }
}
