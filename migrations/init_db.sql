CREATE table if not exists aparelhos_cores(
id integer primary key generated always as identity not null,
idAparelho int NOT NULL,
idCor int NOT NULL 
);

create table if not exists cores(
id integer primary key generated always as identity not null,
cor varchar(50) not null
);

create table if not exists aparelhos(
id integer primary key generated always as identity not null,
idCliente int not null,
idMarcaModeloTipo int not null,
idEstado int not null,
idTecnico int not null,
entrada timestamp not null,
saida timestamp not null,
problema varchar not null,
observacao varchar not null,
orcamento varchar not null,
maoDeObra money not null,
urgencia bit not null,
dataModificacao timestamp
);


create table if not exists clientes (
id integer primary key generated always as identity not null,
nome varchar not null,
dataCadastro timestamp not null,
cpf varchar not null,
sexo char not null,
email varchar,
telefone varchar not null,
whatsapp varchar
);


create table if not exists estados(
id integer primary key generated always as identity not null,
estado varchar not null
);

create table if not exists marcas(
id integer primary key generated always as identity not null,
marca varchar not null
);

create table if not exists modelos (
id integer primary key generated always as identity not null,
modelo varchar not null
);

create table if not exists tecnicos(
id integer primary key generated always as identity not null,
tecnico varchar not null,
numero varchar not null
);

create table if not exists tipos(
id integer primary key generated always as identity not null,
tipo varchar not null
);

create table if not exists contatos(
id integer primary key generated always as identity not null,
idAparelho int not null,
idTecnico int not null,
idTelefone int,
idEstado int not null,
tipo varchar not null,
statusLigacao varchar,
dataContato timestamp not null,
dialogo varchar
);

create table if not exists telefones(
id integer primary key generated always as identity not null,
idCliente int,
numero varchar not null,
whats bit not null,
tipo varchar not null
);

create table if not exists itens(
id integer primary key generated always as identity not null,
idAparelho int not null,
idProduto int not null,
item varchar not null,
valor money not null,
quantidade int not null
);

create table if not exists pagamentos_aparelhos(
id integer primary key generated always as identity not null,
idAparelho int not null,
idPagamento int not null
);

----------- Vendas
create table if not exists compras_produtos(
id integer primary key generated always as identity not null,
idCompra int,
idProduto int,
valorUnidade int,
quantidade int,
subTotal money
);


create table if not exists produtos(
id integer primary key generated always as identity not null,
idCategoria int,
idMarcaModeloTipo int,
produto varchar,
descricao varchar,
valorCompra money,
valorVenda money,
estoque int,
estado bit,
estoqueMin int
);


create table if not exists fornecedores(
id integer primary key generated always as identity not null,
fornecedor varchar,
telefone varchar,
whatsapp varchar,
email varchar
);

create table if not exists compras(
id integer primary key generated always as identity not null,
idFornecedor int,
dataCompra timestamp,
compra varchar,
valorTotal money,
valorFrete money,
notaFiscal varchar
);


create table if not exists categorias(
id integer primary key generated always as identity not null,
categoria varchar
);

create table if not exists vendas(
id integer primary key generated always as identity not null,
idCliente int,
idVendedor int,
dataVenda timestamp,
tipoComprovante varchar,
numeroComprovante varchar,
total money,
totalPago money,
tipoVenda varchar
);

create table if not exists vendedores(
id integer primary key generated always as identity not null,
nome varchar,
email varchar,
telefone varchar,
whatsapp varchar,
sexo char
);

create table if not exists produtos_vendas(
id integer primary key generated always as identity not null,
idVenda int,
idProduto int,
valorVenda money,
quantidade int,
subTotal money
);

create table if not exists pagamentos(
id integer primary key generated always as identity not null,
dataPagamento timestamp not null,
tipoPagamento varchar not null,
valor money not null,
categoria varchar not null
);

create table if not exists vendas_pagamentos(
id integer primary key generated always as identity not null,
idVenda int,
idPagamento int
);

create table if not exists marcas_modelos(
id integer primary key generated always as identity not null,
idMarca int not null,
idModelo int not null
);

create table if not exists marcas_modelos_tipos(
id integer primary key generated always as identity not null,
idMarcaModelo int not null,
idTipo int not null
);


--------- Foreign keys
--aparelhos_cores
alter table aparelhos_cores add constraint fk_aparelhos_cores_aparelhos foreign key (idAparelho) references aparelhos(id);
alter table aparelhos_cores add constraint fk_aparelhos_cores_cores foreign key (idCor) references cores(id);

-- aparelhos
alter table aparelhos add constraint fk_aparelhos_clientes foreign key (idCliente) references clientes(id);

alter table aparelhos add constraint fk_aparelhos_MarcaModeloTipo foreign key (idMarcaModeloTipo) references marcas_modelos_tipos(id);

alter table aparelhos add constraint fk_aparelhos_estados foreign key (idEstado) references estados(id);

alter table aparelhos add constraint fk_aparelhos_tecnico foreign key (idTecnico) references tecnicos(id);

-- marcas_modelos
alter table marcas_modelos  add constraint fk_marcas_modelos_marca foreign key (idMarca) references marcas(id);
alter table marcas_modelos  add constraint fk_marcas_modelos_modelo foreign key (idModelo) references modelos(id);

-- marcas_modelos_tipos
alter table marcas_modelos_tipos add constraint fk_marcas_modelos_tipos_tipos foreign key (idTipo) references tipos(id);
alter table marcas_modelos_tipos add constraint fk_marcas_modelos_tipos_marcaModelo foreign key (idMarcaModelo) references marcas_modelos(id);


-- contatos
alter table contatos add constraint fk_contatos_aparelhos foreign key (idAparelho) references aparelhos(id);

alter table contatos add constraint fk_contatos_tecnicos foreign key (idTecnico) references tecnicos(id);

alter table contatos add constraint fk_contatos_telefones foreign key (idTelefone) references telefones(id);

alter table contatos add constraint fk_contatos_estados foreign key (idEstado) references estados(id);

-- telefones
alter table telefones add constraint fk_telefones_clientes foreign key (idCliente) references clientes(id);

-- itens
alter table itens add constraint fk_itens_aparelhos foreign key (idAparelho) references aparelhos(id);

alter table itens add constraint fk_itens_proutos foreign key (idProduto) references produtos(id);



-- compras_produtos
alter table compras_produtos add constraint fk_compras_produtos_compra foreign key (idCompra) references compras(id);

alter table compras_produtos add constraint fk_compras_produtos_produtos foreign key (idProduto) references produtos(id);

-- produtos
alter table produtos add constraint fk_produtos_categoria foreign key (idCategoria) references categorias(id);

alter table produtos add constraint fk_produtos_marcaModeloTipo foreign key (idMarcaModeloTipo) references marcas_modelos_tipos(id);

-- compras
alter table compras add constraint fk_compras_fornecedores foreign key (idFornecedor) references fornecedores(id);


-- vendas
alter table vendas add constraint fk_vendas_clientes foreign key (idCliente) references clientes(id);

alter table vendas add constraint fk_vendas_vendedores foreign key (idVendedor) references vendedores(id);

-- produtos_vendas
alter table produtos_vendas add constraint fk_produtos_vendas_vendas foreign key (idVenda) references vendas(id);

alter table produtos_vendas add constraint fk_produtos_vendas_produtos foreign key (idProduto) references produtos(id);

-- pagamentos_aparelhos
alter table pagamentos_aparelhos add constraint fk_pagamentos_aparelhos_aparelhos foreign key (idAparelho) references aparelhos(id);
alter table pagamentos_aparelhos add constraint fk_pagamentos_aparelhos_pagamentos foreign key (idPagamento) references pagamentos(id);

-- vendas_pagamentos
alter table vendas_pagamentos add constraint fk_vendas_pagamentos_venda foreign key (idVenda) references vendas(id);
alter table vendas_pagamentos add constraint fk_vendas_pagamentos_pagamentos foreign key (idPagamento) references pagamentos(id);
