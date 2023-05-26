#set page(
    paper: "a4",
    margin:  (x: 1.8cm, y: 1.5cm),
    numbering: "1",
)

#set par(
    justify: true,
)

#set text(
    font: "Linux Libertine",
    size: 11pt,
)

#set heading(numbering: "1.")

#let todo(term) = {
  text(red, box[*TODO: #term*])
}

#align(center, text(17pt)[
  *EcoWork\@Cloud*
])

#grid(
    columns: (1fr, 1fr, 1fr),
    align(center)[
        Diogo Miguel da Silva Santos \
        ist195562 \
        diogosilvasantos\@tecnico.ulisboa.pt \
    ],
    align(center)[
        João Pedro Antunes Aragonez \
        ist195606 \
        joao.aragonez\@tecnico.ulisboa.pt \
    ],
    align(center)[
        Vasco Miguel Cardoso Correia \
        ist194188 \
        #todo[preencher]\@tecnico.ulisboa.pt \
    ],
)

#align(center)[
    #set par(justify: false)
    *Abstract* \ 
    In this report we will discuss our base implementation of an AWS Loadbalancer and Autoscaler for automatic management of the provided Java API
]

#show: rest => columns(2, rest)

= Introduction
#lorem(80)

= Implementation
== Architecture
#lorem(80)
== Data Structures
#lorem(80)
== Algorithms
#lorem(80)


= Remaining Work
== Load Balancer
#lorem(80)
== Auto-Scaler
#lorem(80)

