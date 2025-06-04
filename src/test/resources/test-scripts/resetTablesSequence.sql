SELECT setval(pg_get_serial_sequence('devices', 'id'), coalesce(MAX(id), 1)) from devices;
SELECT setval(pg_get_serial_sequence('customer_contact', 'id'), coalesce(MAX(id), 1)) from customer_contact;
SELECT setval(pg_get_serial_sequence('customers', 'id'), coalesce(MAX(id), 1)) from customers;
SELECT setval(pg_get_serial_sequence('payments', 'id'), coalesce(MAX(id), 1)) from payments;
