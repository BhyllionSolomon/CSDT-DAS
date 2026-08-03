import { useEffect, useState } from "react";
import api from "../services/api";

function DatasetHistory() {

    const [datasets, setDatasets] = useState([]);

    useEffect(() => {

        loadDatasets();

    }, []);

    async function loadDatasets() {

        try {

            const response =
                await api.get("/datasets");

            setDatasets(response.data);

        } catch (err) {

            console.error(err);

        }

    }

    return (

        <div style={{padding:30}}>

            <h1>Dataset History</h1>

            <table
                border="1"
                cellPadding="10"
                style={{
                    width:"100%",
                    borderCollapse:"collapse",
                    marginTop:20
                }}
            >

                <thead>

                <tr>

                    <th>ID</th>

                    <th>Filename</th>

                    <th>Uploaded</th>

                    <th>Status</th>

                    <th>Total Rows</th>

                    <th>Valid Rows</th>

                    <th>Invalid Rows</th>

                </tr>

                </thead>

                <tbody>

                {

                    datasets.map(dataset => (

                        <tr key={dataset.id}>

                            <td>{dataset.id}</td>

                            <td>{dataset.originalFilename}</td>

                            <td>{dataset.uploadedAt}</td>

                            <td>{dataset.status}</td>

                            <td>{dataset.totalRows}</td>

                            <td>{dataset.validRows}</td>

                            <td>{dataset.invalidRows}</td>

                        </tr>

                    ))

                }

                </tbody>

            </table>

        </div>

    );

}

export default DatasetHistory;