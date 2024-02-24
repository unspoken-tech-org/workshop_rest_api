CREATE table if not exists devices_colors(
id integer primary key generated always as identity not null,
idDevice int NOT NULL,
idColor int NOT NULL 
);

create table if not exists colors(
id integer primary key generated always as identity not null,
color varchar(50) not null
);

create table if not exists devices(
id integer primary key generated always as identity not null,
idCustomer int not null,
idBrandModelType int not null,
idDeviceStatus int not null,
idTechnician int not null,
entryDate timestamp not null,
departureDate timestamp not null,
problem varchar not null,
observation varchar not null,
budget varchar not null,
laborValue money not null,
hasUrgency bit not null,
lastUpdate timestamp
);


create table if not exists customers(
id integer primary key generated always as identity not null,
name varchar not null,
insertDate timestamp not null,
cpf varchar not null,
gender char not null,
email varchar,
phone varchar not null,
whatsapp varchar
);


create table if not exists device_status(
id integer primary key generated always as identity not null,
status varchar not null
);

create table if not exists brands(
id integer primary key generated always as identity not null,
brand varchar not null
);

create table if not exists models (
id integer primary key generated always as identity not null,
model varchar not null
);

create table if not exists technicians(
id integer primary key generated always as identity not null,
technician varchar not null,
number varchar not null
);

create table if not exists types(
id integer primary key generated always as identity not null,
type varchar not null
);

create table if not exists customer_contact(
id integer primary key generated always as identity not null,
idDevice int not null,
idTechnician int not null,
idPhone int,
idDeviceStatus int not null,
type varchar not null,
callStatus varchar,
lastContact timestamp not null,
conversation varchar
);

create table if not exists phones(
id integer primary key generated always as identity not null,
idCustomer int,
number varchar not null,
whats bit not null,
type varchar not null
);

create table if not exists items(
id integer primary key generated always as identity not null,
idDevice int not null,
idProduct int not null,
item varchar not null,
itemValue money not null,
quantity int not null
);

create table if not exists payments_devices(
id integer primary key generated always as identity not null,
idDevice int not null,
idPayment int not null
);

----------- Sells
create table if not exists purchases_products(
id integer primary key generated always as identity not null,
idBuying int,
idProduct int,
unitValue int,
quantity int,
subTotal money
);


create table if not exists products(
id integer primary key generated always as identity not null,
idCategory int,
idBrandModelType int,
product varchar,
description varchar,
valueBuying money,
sellingValue money,
supply int,
status bit,
minSupply int
);


create table if not exists suppliers(
id integer primary key generated always as identity not null,
supplier varchar,
phone varchar,
whatsapp varchar,
email varchar
);

create table if not exists purchases(
id integer primary key generated always as identity not null,
idSupplier int,
dataBuying timestamp,
purchase varchar,
totalValue money,
shippingValue money,
invoice varchar
);


create table if not exists categories(
id integer primary key generated always as identity not null,
category varchar
);

create table if not exists sales(
id integer primary key generated always as identity not null,
idCustomer int,
idSeller int,
saleDate timestamp,
invoiceType varchar,
invoiceNumber varchar,
total money,
paidTotal money,
sellType varchar
);

create table if not exists sellers(
id integer primary key generated always as identity not null,
name varchar,
email varchar,
phone varchar,
whatsapp varchar,
gender char
);

create table if not exists products_sales(
id integer primary key generated always as identity not null,
idSell int,
idProduct int,
sellValue money,
quantity int,
subTotal money
);

create table if not exists payments(
id integer primary key generated always as identity not null,
paymentDate timestamp not null,
paymentType varchar not null,
paymentValue money not null,
category varchar not null
);

create table if not exists sales_payments(
id integer primary key generated always as identity not null,
idSell int,
idPayment int
);

create table if not exists brands_models(
id integer primary key generated always as identity not null,
idBrand int not null,
idModel int not null
);

create table if not exists brands_models_types(
id integer primary key generated always as identity not null,
idBrandModel int not null,
idType int not null
);


--------- Foreign keys
--devices_colors
alter table devices_colors add constraint fk_devices_colors_devices foreign key (idDevice) references devices(id);
alter table devices_colors add constraint fk_devices_colors_colors foreign key (idColor) references colors(id);

-- devices
alter table devices add constraint fk_devices_customers foreign key (idCustomer) references customers(id);

alter table devices add constraint fk_devices_BrandModelType foreign key (idBrandModelType) references brands_models_types(id);

alter table devices add constraint fk_devices_device_status foreign key (idDeviceStatus) references device_status(id);

alter table devices add constraint fk_devices_technician foreign key (idTechnician) references technicians(id);

-- brands_models
alter table brands_models  add constraint fk_brands_models_brand foreign key (idBrand) references brands(id);
alter table brands_models  add constraint fk_brands_models_model foreign key (idModel) references models(id);

-- brands_models_types
alter table brands_models_types add constraint fk_brands_models_types_types foreign key (idType) references types(id);
alter table brands_models_types add constraint fk_brands_models_types_brandModel foreign key (idBrandModel) references brands_models(id);


-- customer_contact
alter table customer_contact add constraint fk_customer_contact_devices foreign key (idDevice) references devices(id);

alter table customer_contact add constraint fk_customer_contact_technicians foreign key (idTechnician) references technicians(id);

alter table customer_contact add constraint fk_customer_contact_phones foreign key (idPhone) references phones(id);

alter table customer_contact add constraint fk_customer_contact_device_status foreign key (idDeviceStatus) references device_status(id);

-- phones
alter table phones add constraint fk_phones_customers foreign key (idCustomer) references customers(id);

-- items
alter table items add constraint fk_items_devices foreign key (idDevice) references devices(id);

alter table items add constraint fk_items_products foreign key (idProduct) references products(id);



-- purchases_products
alter table purchases_products add constraint fk_purchases_products_purchase foreign key (idBuying) references purchases(id);

alter table purchases_products add constraint fk_purchases_products_products foreign key (idProduct) references products(id);

-- products
alter table products add constraint fk_products_category foreign key (idCategory) references categories(id);

alter table products add constraint fk_products_brandModelType foreign key (idBrandModelType) references brands_models_types(id);

-- purchases
alter table purchases add constraint fk_purchases_suppliers foreign key (idSupplier) references suppliers(id);


-- sales
alter table sales add constraint fk_sales_customers foreign key (idCustomer) references customers(id);

alter table sales add constraint fk_sales_sellers foreign key (idSeller) references sellers(id);

-- products_sales
alter table products_sales add constraint fk_products_sales_sales foreign key (idSell) references sales(id);

alter table products_sales add constraint fk_products_sales_products foreign key (idProduct) references products(id);

-- payments_devices
alter table payments_devices add constraint fk_payments_devices_devices foreign key (idDevice) references devices(id);
alter table payments_devices add constraint fk_payments_devices_payments foreign key (idPayment) references payments(id);

-- sales_payments
alter table sales_payments add constraint fk_sales_payments_sale foreign key (idSell) references sales(id);
alter table sales_payments add constraint fk_sales_payments_payments foreign key (idPayment) references payments(id);
