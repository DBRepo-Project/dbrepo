"""The broker must receive its verifiable connection username with every tuple."""
from unittest.mock import patch
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]/"lib/python"))
import pandas as pd
from dbrepo.AmqpClient import AmqpClient


def test_both_publish_methods_attach_connection_identity():
    with patch.dict('os.environ', {}, clear=True), patch('pika.BlockingConnection') as connection:
        client = AmqpClient(username='publisher', password='synthetic')
        client.publish('dbrepo.db.table', {'id': 1})
        client.publish_dataframe('dbrepo.db.table', pd.DataFrame([{'id': 2}, {'id': 3}]))
        calls = connection.return_value.channel.return_value.basic_publish.call_args_list
        assert len(calls) == 3
        assert all(c.kwargs['properties'].user_id == 'publisher' for c in calls)
        assert all(c.kwargs['properties'].delivery_mode == 2 for c in calls)


if __name__ == "__main__":
    test_both_publish_methods_attach_connection_identity()
    print("PASS: both AMQP publication paths attach the authenticated connection username and persistent delivery mode")
