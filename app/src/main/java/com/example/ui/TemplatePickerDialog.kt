package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val CardSurface = Color(0xFF2B2930)
private val BorderColor = Color(0xFF49454F)
private val AccentNeonTeal = Color(0xFF80D8CC)
private val AccentElectricBlue = Color(0xFFD0BCFF)

private data class Template(
    val emoji: String,
    val name: String,
    val type: String,
    val description: String,
    val content: String
)

private val templates = listOf(
    Template(
        emoji = "📄",
        name = "Ofício Formal",
        type = "TEXT",
        description = "Modelo de ofício com destinatário, remetente, assunto e corpo formal",
        content = """OFÍCIO Nº ___/____

À [Nome do Destinatário]
Cargo: [Cargo do Destinatário]
Empresa/Órgão: [Nome da Empresa/Órgão]
Endereço: [Endereço Completo]
CEP: [_____-___]

Assunto: [Assunto do Ofício]

Prezado(a) [Nome do Destinatário],

Cumprimentando-o(a) cordialmente, venho por meio deste encaminhar a Vossa Senhoria [descrever o assunto principal do ofício, indicando os motivos, fundamentos legais ou normativos, bem como as solicitações pertinentes].

Esclareço que [detalhamento adicional sobre o assunto, incluindo prazos, valores, documentos anexos ou informações complementares relevantes para o entendimento completo da matéria].

Solicito, ainda, que [instrução ou pedido específico, como providências a serem tomadas, prazos para resposta ou agendamento de reunião].

Na expectativa de seu pronto atendimento, coloco-me à disposição para quaisquer esclarecimentos adicionais.

Atenciosamente,

_________________________
[Nome do Remetente]
[Cargo do Remetente]
[Telefone / E-mail para Contato]

[Local], [dia] de [mês] de [ano]."""
    ),
    Template(
        emoji = "📝",
        name = "Contrato Simples",
        type = "TEXT",
        description = "Contrato de prestação de serviços com cláusulas padrão",
        content = """CONTRATO DE PRESTAÇÃO DE SERVIÇOS

Pelo presente instrumento particular, de um lado:

CONTRATANTE: [Nome Completo ou Razão Social], inscrito(a) no CPF/CNPJ sob o nº [___], com sede/endereço à [Endereço Completo], doravante denominado CONTRATANTE;

CONTRATADO: [Nome Completo ou Razão Social], inscrito(a) no CPF/CNPJ sob o nº [___], com sede/endereço à [Endereço Completo], doravante denominado CONTRATADO;

CLÁUSULA PRIMEIRA - OBJETO
O CONTRATADO se obriga a prestar ao CONTRATANTE os serviços de [descrição detalhada dos serviços], conforme especificações técnicas descritas no Anexo I deste contrato.

CLÁUSULA SEGUNDA - PRAZO E VIGÊNCIA
O presente contrato terá vigência de [___] meses, iniciando-se em [data de início] e encerrando-se em [data de término], podendo ser prorrogado mediante acordo escrito entre as partes.

CLÁUSULA TERCEIRA - VALOR E FORMA DE PAGAMENTO
O CONTRATANTE pagará ao CONTRATADO o valor mensal de R$ [___] ([valor por extenso]), a ser pago até o dia [___] de cada mês, mediante depósito bancário na conta:

Banco: [___]
Agência: [___]
Conta: [___]

CLÁUSULA QUARTA - OBRIGAÇÕES DAS PARTES
4.1. O CONTRATADO se obriga a:
   a) Executar os serviços com diligência e padrão técnico adequado;
   b) Manter sigilo sobre informações confidenciais do CONTRATANTE;
   c) Entregar relatórios mensais de atividades.

4.2. O CONTRATANTE se obriga a:
   a) Fornecer as informações necessárias para a execução dos serviços;
   b) Efetuar os pagamentos nas datas acordadas;
   c) Disponibilizar acesso aos recursos necessários.

CLÁUSULA QUINTA - RESCISÃO
Qualquer das partes poderá rescindir o presente contrato mediante notificação prévia de [___] dias, sem prejuízo das obrigações já vencidas.

CLÁUSULA SEXTA - FORO
Fica eleito o foro da comarca de [Cidade/UF] para dirimir quaisquer litígios oriundos do presente contrato.

E, por estarem justas e contratadas, as partes assinam o presente instrumento em [___] vias de igual teor e forma.

[Local], [dia] de [mês] de [ano].

_________________________
CONTRATANTE

_________________________
CONTRATADO

_________________________
TESTEMUNHA 1

_________________________
TESTEMUNHA 2"""
    ),
    Template(
        emoji = "🧾",
        name = "Recibo",
        type = "TEXT",
        description = "Recibo de pagamento com valor, data e assinatura",
        content = """RECIBO Nº [___]

Recebi(emos) de [Nome Completo do Pagador], inscrito(a) no CPF sob o nº [___], a importância de R$ [___] ([valor por extenso]), referente a [descrição detalhada do pagamento, ex: prestação de serviços de consultoria realizados no mês de referência].

Para clareza, firmo(amos) o presente recibo para todos os fins de direito.

Valor: R$ [___]
Data do pagamento: [dia] de [mês] de [ano]
Forma de pagamento: ( ) Dinheiro ( ) Cheque ( ) Transferência Bancária ( ) Pix ( ) Cartão

_________________________
[Nome Completo do Beneficiário]
CPF: [___]
[Endereço]

_________________________
[Assinatura do Beneficiário]"""
    ),
    Template(
        emoji = "💰",
        name = "Orçamento",
        type = "SPREADSHEET",
        description = "Planilha de orçamento com itens, quantidades e totais",
        content = """Item,Quantidade,Unidade,Valor Unitário (R$),Valor Total (R$)
Serviço de Consultoria,1,mês,5000.00,5000.00
Hospedagem de Site,12,mês,49.90,598.80
Licença de Software,3,unidade,120.00,360.00
Material Gráfico,500,unidade,2.50,1250.00
Deslocamento e Logística,4,viagem,350.00,1400.00
Suporte Técnico,12,mês,197.00,2364.00
Treinamento da Equipe,2,turma,2800.00,5600.00
Manutenção Preventiva,6,serviço,450.00,2700.00
Assinatura de Ferramentas,12,mês,89.00,1068.00
Taxas Administrativas,1,lote,750.00,750.00
TOTAL,,,,22090.80
Desconto (5%),,,-1104.54
Total com Desconto,,,20986.26"""
    ),
    Template(
        emoji = "📅",
        name = "Cronograma",
        type = "SPREADSHEET",
        description = "Planilha de cronograma com datas, tarefas e status",
        content = """Tarefa,Responsável,Data Início,Data Fim,Status,Observações
Definição do Escopo,Equipe de Projeto,02/01/2026,10/01/2026,Concluído,Documento aprovado pelo cliente
Pesquisa de Mercado,Analista de Marketing,12/01/2026,30/01/2026,Concluído,Relatório entregue
Elaboração do Briefing,Coordenador,01/02/2026,05/02/2026,Em Andamento,Aguardando validação
Desenvolvimento do Protótipo,Designer,08/02/2026,28/02/2026,Em Andamento,-
Testes de Qualidade,Analista de QA,01/03/2026,10/03/2026,Pendente,Ambiente será preparado
Implementação,Desenvolvedores,12/03/2026,30/04/2026,Pendente,Aguardar testes
Homologação,Cliente,02/05/2026,10/05/2026,Pendente,-
Treinamento da Equipe,Treinador,12/05/2026,16/05/2026,Pendente,Agendar sala
Lançamento,Equipe de Projeto,20/05/2026,20/05/2026,Pendente,Evento de lançamento agendado
Acompanhamento Pós-Implantação,Suporte,21/05/2026,20/06/2026,Pendente,Relatórios semanais"""
    ),
    Template(
        emoji = "📊",
        name = "Relatório Executivo",
        type = "REPORT",
        description = "Relatório com sumário executivo, análise e recomendações",
        content = """RELATÓRIO EXECUTIVO

Período de Referência: [mês/ano]
Data de Elaboração: [dia] de [mês] de [ano]
Responsável: [Nome do Responsável]
Departamento: [Nome do Departamento]

1. SUMÁRIO EXECUTIVO
O presente relatório tem como objetivo apresentar os resultados, análises e recomendações referentes ao período de [mês/ano] para a [área/projeto]. No período analisado, foram alcançados [___]% das metas estabelecidas, com destaque para [principal resultado positivo] e oportunidades de melhoria em [principal desafio].

2. INDICADORES DE DESEMPENHO
Indicador,Valor Atual,Meta,Variação,Status
Produtividade,87%,90%,-3%,Atenção
Qualidade,94%,92%,+2%,Atingido
Custo (R$),45200.00,48000.00,-5.8%,Atingido
Prazo Médio (dias),12,10,+20%,Atenção
Satisfação do Cliente,4.5/5.0,4.2/5.0,+7%,Superado

3. ANÁLISE DOS RESULTADOS
3.1. Pontos Fortes:
- A qualidade dos entregáveis superou a meta estabelecida, com 94% de aprovação nos testes de controle;
- Os custos operacionais ficaram 5,8% abaixo do orçamento previsto, graças à renegociação de contratos com fornecedores;
- O índice de satisfação dos clientes atingiu 4,5/5,0, superando a meta de 4,2.

3.2. Pontos de Atenção:
- A produtividade ficou 3% abaixo da meta, impactada por [causa identificada, ex: atraso na entrega de insumos];
- O prazo médio de entrega aumentou para 12 dias, em parte devido a retrabalhos no processo de [setor].

4. RECOMENDAÇÕES
- Implementar plano de ação para recuperação da produtividade, com revisão dos processos críticos;
- Automatizar etapas de validação para reduzir o prazo médio de entrega;
- Manter as boas práticas de gestão de custos e qualidade;
- Realizar treinamento da equipe nos pontos identificados como gargalos.

5. PRÓXIMOS PASSOS
- Aprovação do plano de ação pelo comitê executivo até [data];
- Início das implementações em [data];
- Próximo relatório: [data prevista].

________________________
Assinatura do Responsável"""
    ),
    Template(
        emoji = "🗣️",
        name = "Ata de Reunião",
        type = "MINUTE",
        description = "Ata com pauta, participantes, discussões e encaminhamentos",
        content = """ATA DE REUNIÃO Nº [___]

DATA: [dia] de [mês] de [ano]
HORÁRIO: [__]:[__] às [__]:[__]
LOCAL: [Sala/Plataforma Virtual]
TIPO: (X) Ordinária ( ) Extraordinária

1. PARTICIPANTES
   Presentes:
   - [Nome], [Cargo/Função]
   - [Nome], [Cargo/Função]
   - [Nome], [Cargo/Função]
   - [Nome], [Cargo/Função]
   - [Nome], [Cargo/Função]

   Ausentes:
   - [Nome], [Cargo/Função] - justificado

2. PAUTA
   2.1. Aprovação da ata da reunião anterior;
   2.2. [Tópico 1 - descrição];
   2.3. [Tópico 2 - descrição];
   2.4. [Tópico 3 - descrição];
   2.5. Assuntos gerais.

3. DISCUSSÕES E DELIBERAÇÕES
   3.1. A ata da reunião anterior foi aprovada por unanimidade.
   3.2. [Tópico 1] - [Nome] apresentou os resultados referentes a [assunto]. Após debate, ficou decidido que [decisão tomada]. [Nome] sugeriu [proposta], que foi acatada pelo grupo.
   3.3. [Tópico 2] - Foi discutido o andamento de [projeto/ação]. [Nome] informou que [status atual]. O grupo deliberou por [decisão].
   3.4. [Tópico 3] - [Nome] trouxe à pauta a questão de [assunto]. Ficou acordado que [encaminhamento].
   3.5. Assuntos gerais: [breve relato de outros assuntos tratados].

4. ENCAMINHAMENTOS E PRAZOS
   | # | Ação | Responsável | Prazo |
   |---|------|-------------|-------|
   | 1 | [Ação 1] | [Nome] | [data] |
   | 2 | [Ação 2] | [Nome] | [data] |
   | 3 | [Ação 3] | [Nome] | [data] |
   | 4 | [Ação 4] | [Nome] | [data] |

5. PRÓXIMA REUNIÃO
   Data: [dia] de [mês] de [ano]
   Horário: [__]:[__]
   Local: [Sala/Plataforma Virtual]

Nada mais havendo a tratar, foi lavrada a presente ata, que será assinada pelos presentes e arquivada.

_________________________
[Nome do Secretário]
Secretário(a) da Reunião"""
    )
)

@Composable
fun TemplatePickerDialog(
    onDismiss: () -> Unit,
    onSelectTemplate: (title: String, type: String, content: String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "TEMPLATES DE DOCUMENTOS",
                    color = AccentNeonTeal,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Escolha um modelo profissional para começar",
                    color = Color(0xFFCAC4D0),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(420.dp)
                ) {
                    items(templates) { template ->
                        TemplateCard(
                            template = template,
                            onClick = {
                                onSelectTemplate(template.name, template.type, template.content)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: Template,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = template.emoji,
                fontSize = 28.sp,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = template.description,
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
            ) {
                Text(
                    text = template.type,
                    color = AccentElectricBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
