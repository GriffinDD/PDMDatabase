# Group 31
# Griffin Danner-Doran gtd6864
# Aditya Kumar ak6169
# Soban Mahmud sm9614
# Donald Tsang dht1455
# Veronika Zsenits vmz5751

import os
import psycopg2
from sshtunnel import SSHTunnelForwarder

ssh_host = "starbug.cs.rit.edu"
ssh_port = 22
# When running the code, credentials were hardcoded. To upload the data, one would
# have to insert their SSH username and SSH password. If their credentials are valid,
# the code will proceed to load the given csvs into the PostgreSQL database.

ssh_username = "" #SSH username
ssh_password = "" #SSH password
sql_host = "127.0.0.1"
sql_port = 5432            
sql_db = 'p32001_31'
# (USUALLY) the same as the SSH. Edit if needed.
sql_user = ssh_username # SQL username
sql_password = ssh_password # SQL password

# Order to load in csvs, done primary tables first, then dependent tables.
load_order = ["person",
              "movie",
              "releaseplatform",
              "studio",
              "genre",
              "directs",
              "actsin",
              "releasedon",
              "genreof",
              "produces",
              "users",
              "collection",
              "rates",
              "watches",
              "partof",
              "follows"]

# Was run in same directory as data to populate database.
current_directory = os.path.dirname(os.path.abspath(__file__))

with SSHTunnelForwarder( # creates SSH Tunnel
    (ssh_host, ssh_port),
    ssh_username=ssh_username,
    ssh_password=ssh_password,
    remote_bind_address=(sql_host, sql_port),
) as tunnel:
    local_bind_port = tunnel.local_bind_port
    conn_params = {
        'dbname': sql_db,
        'user': sql_user,
        'password': sql_password,
        'host': sql_host,
        'port': local_bind_port
    }

# try/except/finally statement to handle any potential exceptions that occur.
    try:
        conn = psycopg2.connect(**conn_params)
        cursor = conn.cursor()
        for table_name in load_order:
            csv_filename = f"{table_name}_sample_data.csv" # all data needs to remain consistent, from csv file to database table name
            csv_file_path = os.path.join(current_directory, csv_filename)

            if os.path.exists(csv_file_path):
                try:
                    with open(csv_file_path, 'r', encoding='utf-8') as f:
                        next(f)
                        # inline SQL statement to copy CSV data into SQL
                        cursor.copy_expert(f"""
                            COPY {table_name} FROM STDIN WITH CSV ENCODING 'UTF-8'
                        """, f)
                    conn.commit()
                    print(f"Data loaded successfully into table: {table_name}")
                except Exception as e:
                    conn.rollback()
                    print(f"Error loading data into table {table_name}: {e}")
            else:
                print(f"CSV file not found for table: {table_name}")

    except Exception as e:
        print(f"Error connecting to PostgreSQL: {e}")
    finally:
        if conn:
            conn.close()
