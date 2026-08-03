import { useEffect, useState } from "react";
import api from "../services/api";

function DiseaseRecords() {

    const [records, setRecords] = useState([]);

    useEffect(() => {

        loadRecords();

    }, []);

    function loadRecords() {

        api.get("/disease-records")
            .then((response) => {

                console.log(response.data);

                setRecords(response.data);

            })
            .catch((error) => {

                console.error(error);

            });

    }

    return (

        <div>

            <h1>Disease Records</h1>

            <p>Total Records : {records.length}</p>

            <table border="1">

                <thead>

                <tr>

                    <th>ID</th>
                    <th>Disease</th>
                    <th>State</th>
                    <th>LGA</th>
                    <th>Year</th>
                    <th>Week</th>
                    <th>Suspected</th>
                    <th>Confirmed</th>
                    <th>Deaths</th>

                </tr>

                </thead>

                <tbody>

                {

                    records.map(record => (

                        <tr key={record.id}>

                            <td>{record.id}</td>
                            <td>{record.diseaseName}</td>
                            <td>{record.state}</td>
                            <td>{record.lga}</td>
                            <td>{record.year}</td>
                            <td>{record.epiWeek}</td>
                            <td>{record.suspectedCases}</td>
                            <td>{record.confirmedCases}</td>
                            <td>{record.deaths}</td>

                        </tr>

                    ))

                }

                </tbody>

            </table>

        </div>

    );

}

export default DiseaseRecords;