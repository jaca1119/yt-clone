import { useEffect } from "react";
import { handleCallback } from "../scripts/auth";

export default function Callback() {
  useEffect(() => {
    handleCallback();
  });

  return <div>Callback</div>;
}
