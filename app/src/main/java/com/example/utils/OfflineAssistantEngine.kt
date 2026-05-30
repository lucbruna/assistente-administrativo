package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OfflineAssistantEngine {

    fun generateLocalResponse(prompt: String, type: String): String {
        val cleanPrompt = prompt.lowercase(Locale.getDefault())
        val dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        return when (type) {
            "SPREADSHEET" -> generateOfflineSpreadsheet(cleanPrompt)
            "REPORT" -> generateOfflineReport(prompt, dateString)
            "MINUTE" -> generateOfflineMinute(prompt, dateString)
            "TEXT" -> generateOfflineText(prompt, dateString)
            "GENERAL" -> generateOfflineAnswer(cleanPrompt)
            else -> generateOfflineAnswer(cleanPrompt)
        }
    }

    private fun generateOfflineSpreadsheet(cleanPrompt: String): String {
        return when {
            cleanPrompt.contains("finan") || cleanPrompt.contains("caixa") || cleanPrompt.contains("dinheiro") || cleanPrompt.contains("gasto") -> {
                """
                Data,Descrição,Entrada (R$),Saída (R$),Saldo (R$)
                01/05/2026,Saldo Inicial,2500.00,0.00,2500.00
                05/05/2026,Venda Serviço A,1200.00,0.00,3700.00
                10/05/2026,Assinatura Software,0.00,150.00,3550.00
                15/05/2026,Material de Escritório,0.00,85.50,3464.50
                20/05/2026,Honorários Recebidos,3000.00,0.00,6464.50
                25/05/2026,Conta de Energia,0.00,320.00,6144.50
                """.trimIndent()
            }
            cleanPrompt.contains("hora") || cleanPrompt.contains("ponto") || cleanPrompt.contains("func") || cleanPrompt.contains("colaborador") -> {
                """
                Colaborador,Dia da Semana,Entrada,Saída,Horas Extras
                Alessandro Santos,Segunda-feira,08:00,18:00,1.0
                Alessandro Santos,Terça-feira,08:00,17:00,0.0
                Camila Oliveira,Segunda-feira,09:00,18:00,0.0
                Camila Oliveira,Terça-feira,09:00,19:30,1.5
                Rodrigo Mendes,Segunda-feira,08:00,17:00,0.0
                Rodrigo Mendes,Terça-feira,08:00,18:00,1.0
                """.trimIndent()
            }
            cleanPrompt.contains("estoque") || cleanPrompt.contains("inventario") || cleanPrompt.contains("produto") || cleanPrompt.contains("pecas") -> {
                """
                Código,Produto,Categoria,Qtd em Estoque,Preço Unit. (R$),Valor Total (R$)
                PROD-101,Papel Sulfite A4 Caixa,Material,45,35.00,1575.00
                PROD-102,Caneta Esferográfica Azul,Escrita,150,1.50,225.00
                PROD-103,Grampeador de Metal G1,Ferramentas,12,42.00,504.00
                PROD-104,Pasta Suspensa Plástica,Arquivo,80,4.80,384.00
                PROD-105,Toner Impressora Laser,Informática,8,220.00,1760.00
                """.trimIndent()
            }
            cleanPrompt.contains("venda") || cleanPrompt.contains("clien") || cleanPrompt.contains("comercial") -> {
                """
                ID Venda,Cliente,Data,Produto,Valor Unitário (R$),Qtd,Total (R$)
                VND-5001,Construtora Civilis,02/05/2026,Planilha de Custos,120.00,2,240.00
                VND-5002,Clínica Sorriso,08/05/2026,Treinamento IA,1500.00,1,1500.00
                VND-5003,Escritório Advocacia,12/05/2026,Suporte Técnico,300.00,3,900.00
                VND-5004,Mercado Central,18/05/2026,Organização Arquivo,80.00,5,400.00
                """.trimIndent()
            }
            else -> {
                """
                Item,Categoria,Quantidade,Status,Responsável,Prazo
                Revisão de Arquivo,Administração,1,Em andamento,Alessandro,29/05/2026
                Feedback Mensal,Recursos Humanos,5,Pendente,Gerência,05/06/2026
                Inventário Físico,Almoxarifado,1,Concluído,Camila,20/05/2026
                Ajuste Contábil,Financeiro,1,Em andamento,Rodrigo,31/05/2026
                """.trimIndent()
            }
        }
    }

    private fun generateOfflineReport(prompt: String, date: String): String {
        return """
        ========================= RELATÓRIO EXECUTIVO DE ESCRITÓRIO =========================
        Assunto: ${prompt.replace("relatorio", "", true).replace("relatório", "", true).trim().ifEmpty { "Análise de Atividades Diárias" }}
        Data de Emissão: $date
        Classificação: Uso Interno Restrito
        -------------------------------------------------------------------------------------

        1. OBJETIVO DO DOCUMENTO
        Este relatório foi compilado localmente pelo Assistente de Escritório Inteligente com a finalidade de fornecer uma análise estruturada, clara e acionável sobre o tema solicitado pelo operador.

        2. ANÁLISE SITUACIONAL E DESENVOLVIMENTO
        - Processamento Ativo: Os fluxos de trabalho foram avaliados com base nos insumos do departamento.
        - Controle de Eficiência: Foi identificado um potencial de aumento de 18% na produtividade com a automação de arquivamento digital e indexação de dados.
        - Segurança da Informação: Toda a base de dados permanece guardada de forma segura e offline no aparelho local, diminuindo vulnerabilidades cibernéticas externas.

        3. CONCLUSÕES E RECOMENDAÇÕES (DIRETRIZES DE AÇÃO)
        - Recomenda-se a revisão periódica das planilhas integradas do escritório para evitar divergências.
        - Estimular a equipe a transcrever as decisões de reuniões em Atas de formato oficial (disponíveis nestas mesmas ferramentas).
        - Prosseguir com a digitalização de documentos operacionais antigos do escritório.

        -------------------------------------------------------------------------------------
        Elaborado de forma automática por: Assistente Inteligente de Escritório (Local Engine)
        """.trimIndent()
    }

    private fun generateOfflineMinute(prompt: String, date: String): String {
        return """
        ================================ ATA DE REUNIÃO ORDINÁRIA ================================
        Pauta: ${prompt.replace("ata", "", true).replace("ata de reuniao", "", true).trim().ifEmpty { "Alinhamento de Processos e Metas" }}
        Data da Reunião: $date
        Local: Presencial (Escrito Principal)
        ------------------------------------------------------------------------------------------

        PARTICIPANTES:
        - Diretor Geral (Presidência)
        - Líder de Operações Administrativas
        - Secretário Executivo da Ata
        - Integrantes da Equipe Operacional

        DISCUSSÕES E DELIBERAÇÕES DA REUNIÃO:
        1. Alinhamento Inicial: Feita a leitura das pendências da pauta anterior.
        2. Produtividade Digital: Discutiu-se a necessidade do uso de Inteligência Artificial local no dispositivo móvel para gerar e estruturar rascunhos de relatórios administrativos rapidamente.
        3. Operações e Planilhas: Foi definida a obrigatoriedade de importação rápida de tabelas via formato CSV para o sistema local a fim de facilitar cálculos internos.

        ENCAMINHAMENTOS E DESIGNADOS (AÇÕES FUTURAS):
        - Ação 1: Alessandro Santos deve revisar os ativos do escritório até a próxima quarta-feira.
        - Ação 2: Camila Oliveira atualizará a planilha financeira consolidada correspondente ao período.
        - Ação 3: Envio desta ata formalizada para ciência e assinatura digital coletiva dos participantes.

        ------------------------------------------------------------------------------------------
        Ata gerada e homologada localmente pelo Assistente de Escritório Inteligente.
        """.trimIndent()
    }

    private fun generateOfflineText(prompt: String, date: String): String {
        return """
        MEMORANDO ADMINISTRATIVO N° ${SimpleDateFormat("yyMM", Locale.getDefault()).format(Date())}-01
        
        Remetente: Departamento Administrativo Integrado
        Destinatário: Equipe de Operações e Secretariado
        Data de Emissão: $date
        Assunto: Rascunho / Ofício Administrativo - Referência: ${prompt.replace("redacao", "", true).replace("redação", "", true).replace("texto", "", true).trim().ifEmpty { "Informações Gerais e Alinhamentos" }}

        Prezados,

        Através deste documento oficial, rascunhado por meio do sistema inteligente local, comunicamos as orientações de conformidade técnica e operacional para todo o setor:

        1. A redação de novos ofícios deve seguir rigorosamente o padrão de recuo de margem estabelecido pelo manual de redação corporativo.
        2. Toda e qualquer planilha montada contendo dados sensíveis deve ser tratada exclusivamente no ambiente local restrito para garantir sigilo total e conformidade com a LGPD.
        3. Deixamos ciente que os relatórios mensais de metas serão requeridos na data limite pactuada em ata prévia.

        Permanecemos à disposição em caso de eventuais esclarecimentos complementares.

        Atenciosamente,

        ___________________________________
        Gestão Executiva e Secretariado Geral
        """.trimIndent()
    }

    private fun generateOfflineAnswer(prompt: String): String {
        return when {
            prompt.contains("bom dia") || prompt.contains("boa tarde") || prompt.contains("boa noite") -> {
                """
                Olá! 😊 Bom dia! Como posso ajudar você hoje?

                Estou aqui para:
                • Criar planilhas, relatórios, atas e documentos
                • Responder perguntas sobre diversos assuntos
                • Digitalizar documentos com OCR
                • Entre muitos outros recursos!

                É só me falar o que precisa!
                """.trimIndent()
            }
            prompt.startsWith("bom") || prompt.startsWith("boa") || prompt.contains("ola")
                || prompt.contains("olá") || prompt.contains("oi") || prompt.contains("tudo bem")
                || prompt.contains("como vai") || prompt.contains("blz") || prompt.contains("beleza")
                || prompt.contains("opa") || prompt.contains("hey") -> {
                """
                Olá! Tudo bem? 😊

                Sou seu assistente de escritório inteligente! Posso ajudar com:
                • Documentos e planilhas
                • OCR e scanner
                • Relatórios e atas
                • Cálculos e organização

                O que você gostaria de fazer hoje?
                """.trimIndent()
            }
            prompt.contains("obrigado") || prompt.contains("valeu") || prompt.contains("brigado") -> {
                """
                Por nada! 😊 Fico feliz em ajudar!

                Se precisar de mais alguma coisa, é só chamar. Estou sempre à disposição!
                """.trimIndent()
            }
            prompt.contains("qual seu nome") || prompt.contains("quem é você") || prompt.contains("quem é voce") -> {
                """
                Sou o Assistente de Escritório Inteligente! 🚀

                Fui criado para ajudar você no dia a dia do escritório com:
                - Criação de documentos, planilhas, atas e relatórios
                - Reconhecimento de texto por imagem (OCR)
                - Fusão de documentos com IA
                - Geração de gráficos
                - Transcrição por voz
                - Backup e restauração de dados
                - E muito mais!

                Como posso te ajudar hoje?
                """.trimIndent()
            }
            prompt.contains("excel") || prompt.contains("planilha") || prompt.contains("formula") || prompt.contains("procv") || prompt.contains("fórmula") -> {
                """
                Dicas de Excel / Planilhas Administrativa (Offline Helper):
                
                1. PROCV (VLOOKUP): Localiza informações em colunas.
                   Fórmula: =PROCV(valor_procurado; matriz_tabela; indice_coluna; [procurar_intervalo])
                   
                2. SOMA (SUM): Soma um intervalo numérico completo.
                   Fórmula: =SOMA(A1:A50)
                   
                3. SE (IF): Retorna valores com base em critérios lógicos.
                   Fórmula: =SE(B1>1000; "Acima do Orçamento"; "Dentro do Limite")
                   
                Segurança: Use sempre nossa funcionalidade de Importar e Exportar CSV nesta ferramenta para ter maior agilidade com dados.
                """.trimIndent()
            }
            prompt.contains("ata") || prompt.contains("reuni") -> {
                """
                Dica de Como Redigir Ata de Reunião de Qualidade:
                
                - Insira dia, horário inicial, lista de presentes e o gestor da pauta.
                - Foque nos combinados e nas tomadas de decisão. Evite textos decorativos excessivos.
                - Registre quem é o responsável ("Dono") de cada tarefa e o prazo de entrega estipulado.
                - Finalize formalmente, permitindo compartilhamento fácil por PDF.
                - Utilize nossa ação direta de 'Criar Ata' no painel principal!
                """.trimIndent()
            }
            prompt.contains("segurança") || prompt.contains("seguro") || prompt.contains("dados") || prompt.contains("offline") -> {
                """
                Segurança de Dados do Assistente MultiOficina:
                
                Este aplicativo opera de maneira estritamente LOCAL no seu celular.
                - Todas as planilhas, relatórios e redações gerados são guardados na memória do seu próprio aparelho no banco de dados SQLite (Room).
                - O acesso restrito offline assegura que seus dados empresariais confidenciais permaneçam inacessíveis a softwares espiões em nuvens de terceiros.
                - Exportar relatórios para PDF e planilhas para CSV garante flexibilidade profissional mantendo você sob controle integral do seu material de trabalho.
                """.trimIndent()
            }
            else -> {
                """
                Assistente Inteligente (Modo Offline):
                
                Estou pronto para ajudar com qualquer assunto! Como estamos offline, minha base de conhecimento local inclui:
                
                - 📊 Planilhas Financeiras, Vendas e Estoques
                - 📝 Atas Oficiais de reunião e Relatórios Executivos
                - 📄 Redação de documentos, ofícios e memorandos
                - 💡 Dicas de Excel, produtividade, organização empresarial
                - 🔒 Segurança da informação e boas práticas corporativas
                
                Para perguntas mais complexas ou específicas de outras áreas, ative o modo online 'Gemini' no menu superior para respostas mais abrangentes e flexíveis.
                """.trimIndent()
            }
        }
    }
}
